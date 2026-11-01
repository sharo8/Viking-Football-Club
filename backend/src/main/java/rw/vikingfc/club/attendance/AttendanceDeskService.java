package rw.vikingfc.club.attendance;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.ClubDates;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.medical.Injury;
import rw.vikingfc.club.medical.InjuryRepository;
import rw.vikingfc.club.medical.InjuryStatus;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FinePolicy;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.sanction.FineService;
import rw.vikingfc.club.sanction.FineStatus;
import rw.vikingfc.club.sanction.FineType;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.staff.StaffMember;
import rw.vikingfc.club.staff.StaffMemberRepository;
import rw.vikingfc.club.training.SessionType;
import rw.vikingfc.club.training.TrainingSession;
import rw.vikingfc.club.training.TrainingSessionRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceDeskService {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    private final TrainingSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AttendanceRecordHistoryRepository historyRepository;
    private final AttendanceJustificationRepository justificationRepository;
    private final PlayerRepository playerRepository;
    private final StaffMemberRepository staffRepository;
    private final InjuryRepository injuryRepository;
    private final FootballMatchRepository matchRepository;
    private final FineRepository fineRepository;
    private final FineService fineService;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final AttendanceReport report;
    private final Clock clock;
    private final DataFormatter formatter = new DataFormatter();

    @Transactional(readOnly = true)
    public Map<String, Object> sessions(String audience, String preset, LocalDate from, LocalDate to, String kind,
                                        String status, String q, int page, int size) {
        String who = audience(audience);
        if ("STAFF".equals(who) && !canWrite("STAFF")) {
            Map<String, Object> locked = page(List.of(), page, size, counts());
            locked.put("locked", true);
            return locked;
        }
        LocalDate[] bounds = AttendanceRange.bounds(preset, from, to, LocalDate.now(clock));
        List<Map<String, Object>> items = new ArrayList<>();
        for (TrainingSession session : seasonSessions()) {
            if (!who.equals(session.getAudience())) {
                continue;
            }
            if (!AttendanceRange.in(session.getSessionDate(), bounds[0], bounds[1])) {
                continue;
            }
            if (kind != null && !kind.isBlank() && !"all".equalsIgnoreCase(kind) && !kind.equalsIgnoreCase(session.getKind())) {
                continue;
            }
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status) && !status.equalsIgnoreCase(session.getDeskStatus())) {
                continue;
            }
            Map<String, Object> card = card(session);
            if (q != null && !q.isBlank() && !String.valueOf(card).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(card);
        }
        items.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("date"))).reversed());
        return page(items, page, size, counts());
    }

    public Map<String, Object> create(AttendanceDeskDtos.SessionWrite write) {
        String who = audience(write.audience());
        assertWrite(who);
        TrainingSession session = store(new TrainingSession(), write);
        auditService.record("ATTENDANCE_SESSION", "TrainingSession", session.getId(), null, write.kind(), null);
        return detail(session.getId());
    }

    public Map<String, Object> update(Long id, AttendanceDeskDtos.SessionWrite write) {
        TrainingSession session = require(id);
        assertWrite(session.getAudience());
        assertEditable(session, "Session updated");
        if (!session.getAudience().equals(audience(write.audience())) && !attendanceRepository.findBySessionId(id).isEmpty()) {
            throw BusinessException.conflict("Attendance is already recorded for this audience");
        }
        store(session, write);
        auditService.record("ATTENDANCE_SESSION_UPDATE", "TrainingSession", id, null, write.kind(), null);
        return detail(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        TrainingSession session = require(id);
        assertRead(session.getAudience());
        Map<String, Object> body = card(session);
        body.put("notes", session.getNotes());
        body.put("roster", rosterRows(session));
        body.put("thresholds", thresholds());
        body.put("access", access());
        return body;
    }

    public Map<String, Object> saveMarks(Long id, AttendanceDeskDtos.MarksWrite write) {
        TrainingSession session = require(id);
        assertWrite(session.getAudience());
        assertEditable(session, write.reason());
        if (write.marks() != null) {
            for (AttendanceDeskDtos.MarkWrite mark : write.marks()) {
                applyMark(session, mark, write.reason());
            }
        }
        sessionRepository.save(session);
        auditService.record("ATTENDANCE_SAVE", "TrainingSession", id, null, write.marks() == null ? 0 : write.marks().size(), null);
        Map<String, Object> body = detail(id);
        body.put("savedAt", Instant.now(clock).toString());
        return body;
    }

    public Map<String, Object> close(Long id) {
        TrainingSession session = require(id);
        assertWrite(session.getAudience());
        session.setDeskStatus("CLOSED");
        session.setClosedBy(actor());
        session.setClosedAt(Instant.now(clock));
        sessionRepository.save(session);
        auditService.record("ATTENDANCE_CLOSE", "TrainingSession", id, null, session.getSessionDate(), null);
        return detail(id);
    }

    public Map<String, Object> reopen(Long id, String reason) {
        TrainingSession session = require(id);
        assertClosedEditor();
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reason is required", "REASON_REQUIRED");
        }
        session.setDeskStatus("OPEN");
        session.setNotes(append(session.getNotes(), "Reopened: " + reason.trim()));
        sessionRepository.save(session);
        auditService.record("ATTENDANCE_REOPEN", "TrainingSession", id, null, reason.trim(), null);
        return detail(id);
    }

    public void delete(Long id, String reason) {
        TrainingSession session = require(id);
        assertClosedEditor();
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reason is required", "REASON_REQUIRED");
        }
        for (Fine fine : fineRepository.findByTrainingSessionId(id)) {
            if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.PARTIALLY_PAID) {
                throw BusinessException.conflict("A paid fine is linked to this session");
            }
            fineRepository.delete(fine);
        }
        fineRepository.flush();
        List<AttendanceRecord> records = attendanceRepository.findBySessionId(id);
        for (AttendanceRecord record : records) {
            historyRepository.deleteAll(historyRepository.findByRecord_IdOrderByChangedAtAsc(record.getId()));
        }
        historyRepository.flush();
        justificationRepository.deleteAll(justificationRepository.findBySession_Id(id));
        justificationRepository.flush();
        attendanceRepository.deleteAll(records);
        attendanceRepository.flush();
        sessionRepository.delete(session);
        auditService.record("ATTENDANCE_DELETE", "TrainingSession", id, null, reason.trim(), null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> grid(String audience, String preset, LocalDate from, LocalDate to, String grain, String q,
                                    String position, String line, String role, String department, String mark, String kind,
                                    String sessionStatus, Boolean hasFine, Boolean edited, String recordedBy) {
        return grid(audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> grid(String audience, String preset, LocalDate from, LocalDate to, String grain, String q,
                                    String position, String line, String role, String department, String mark, String kind,
                                    String sessionStatus, Boolean hasFine, Boolean edited, String recordedBy, String players) {
        String who = audience(audience);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("counts", counts());
        body.put("access", access());
        body.put("thresholds", thresholds());
        if ("STAFF".equals(who) && !canWrite("STAFF")) {
            body.put("locked", true);
            body.put("sessions", List.of());
            body.put("people", List.of());
            body.put("groups", List.of());
            body.put("totals", Map.of());
            return body;
        }
        assertRead(who);
        LocalDate[] bounds = AttendanceRange.bounds(preset, from, to, LocalDate.now(clock));
        List<TrainingSession> sessions = filteredSessions(who, bounds, kind, sessionStatus);
        Map<Long, Fine> fines = finesByRecord();
        List<Map<String, Object>> people = new ArrayList<>();
        for (Person person : roster(who)) {
            people.add(personRow(person, sessions, fines));
        }
        people = people.stream().filter(row -> keep(row, q, position, line, role, department, mark, hasFine, edited, recordedBy)).toList();
        if (players != null && !players.isBlank()) {
            java.util.Set<String> ids = java.util.Set.of(players.split(","));
            people = people.stream().filter(row -> ids.contains(String.valueOf(row.get("personId")))).toList();
        }
        body.put("sessions", columns(sessions));
        body.put("groups", groups(sessions, grain));
        body.put("people", people);
        body.put("totals", totals(people, sessions));
        body.put("shown", people.size());
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(String audience, String preset, LocalDate from, LocalDate to, String grain, String q,
                                     String position, String line, String role, String department, String mark, String kind,
                                     String sessionStatus, Boolean hasFine, Boolean edited, String recordedBy, String compare) {
        Map<String, Object> grid = grid(audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> people = (List<Map<String, Object>>) grid.get("people");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) grid.get("sessions");
        ClubSettings settings = settingsService.current();
        int present = 0;
        int late = 0;
        int absent = 0;
        int excused = 0;
        BigDecimal fines = BigDecimal.ZERO;
        Map<String, int[]> buckets = new LinkedHashMap<>();
        for (Map<String, Object> person : people) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) person.get("cells");
            for (Map<String, Object> session : sessions) {
                Map<String, Object> cell = cells.get(String.valueOf(session.get("id")));
                if (cell == null || cell.get("mark") == null || String.valueOf(cell.get("mark")).isBlank()) {
                    continue;
                }
                String status = String.valueOf(cell.get("mark"));
                switch (status) {
                    case "P" -> present++;
                    case "L" -> late++;
                    case "A" -> absent++;
                    case "E" -> excused++;
                    default -> {
                    }
                }
                if (cell.get("fine") instanceof BigDecimal amount) {
                    fines = fines.add(amount);
                }
                String bucket = AttendanceRange.bucket(LocalDate.parse(String.valueOf(session.get("date"))), grain);
                int[] counts = buckets.computeIfAbsent(bucket, key -> new int[4]);
                switch (status) {
                    case "P" -> counts[0]++;
                    case "L" -> counts[1]++;
                    case "A" -> counts[2]++;
                    case "E" -> counts[3]++;
                    default -> {
                    }
                }
            }
        }
        List<Map<String, Object>> stacked = new ArrayList<>();
        List<Map<String, Object>> trend = new ArrayList<>();
        buckets.forEach((label, counts) -> {
            Map<String, Object> bar = new LinkedHashMap<>();
            bar.put("label", label);
            bar.put("P", counts[0]);
            bar.put("L", counts[1]);
            bar.put("A", counts[2]);
            bar.put("E", counts[3]);
            stacked.add(bar);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", label);
            point.put("rate", AttendanceCalculator.percentage(counts[0], counts[1], counts[3], counts[2]));
            trend.add(point);
        });
        int atRisk = 0;
        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Map<String, Object> person : people) {
            if ("At risk".equals(person.get("band"))) {
                atRisk++;
            }
            if (((Number) person.get("counted")).intValue() > 0) {
                ranked.add(person);
            }
        }
        ranked.sort(Comparator.comparing((Map<String, Object> row) -> new BigDecimal(row.get("rate").toString())).reversed());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rate", AttendanceCalculator.percentage(present, late, excused, absent));
        body.put("punctuality", AttendanceCalculator.percentage(present, 0, 0, late + absent + excused));
        body.put("sessions", sessions.size());
        body.put("absences", absent);
        body.put("late", late);
        body.put("finesIssued", fines);
        body.put("atRisk", atRisk);
        body.put("onTrack", settings.getAttendanceOnTrackPct());
        body.put("watch", settings.getAttendanceWatchPct());
        body.put("stacked", stacked);
        body.put("trend", trend);
        body.put("donut", List.of(slice("P", present), slice("L", late), slice("A", absent), slice("E", excused)));
        body.put("byPosition", groupRates(people, "position"));
        body.put("byLine", groupRates(people, "STAFF".equals(audience(audience)) ? "department" : "line"));
        body.put("byRole", groupRates(people, "STAFF".equals(audience(audience)) ? "role" : "position"));
        body.put("heatmap", heatmap(people, sessions));
        body.put("best", ranked.stream().limit(5).map(this::rank).toList());
        List<Map<String, Object>> worst = new ArrayList<>(ranked);
        worst.sort(Comparator.comparing(row -> new BigDecimal(row.get("rate").toString())));
        body.put("worst", worst.stream().limit(5).map(this::rank).toList());
        body.put("atRiskList", people.stream().filter(row -> "At risk".equals(row.get("band"))).map(this::rank).toList());
        body.put("compare", compare(compare, people, sessions, grain));
        body.put("locked", Boolean.TRUE.equals(grid.get("locked")));
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(String type, Long id, LocalDate from, LocalDate to) {
        Person person = person(type, id);
        List<AttendanceRecord> records = recordsOf(person).stream()
                .filter(record -> AttendanceRange.in(record.getSession().getSessionDate(), from, to))
                .sorted(Comparator.comparing(record -> record.getSession().getSessionDate()))
                .toList();
        int[] counts = count(records);
        List<Fine> fines = finesOf(person);
        BigDecimal issued = fines.stream().filter(fine -> fine.getStatus() != FineStatus.WAIVED).map(Fine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = fines.stream().map(Fine::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal open = fines.stream().filter(fine -> fine.getStatus() == FineStatus.OUTSTANDING || fine.getStatus() == FineStatus.PARTIALLY_PAID)
                .map(Fine::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, int[]> months = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            if (record.getStatus() == AttendanceMark.I || record.getStatus() == AttendanceMark.N) {
                continue;
            }
            String key = record.getSession().getSessionDate().getYear() + "-" + String.format("%02d", record.getSession().getSessionDate().getMonthValue());
            int[] bucket = months.computeIfAbsent(key, ignored -> new int[4]);
            add(bucket, record.getStatus());
        }
        String best = null;
        String worst = null;
        BigDecimal bestRate = null;
        BigDecimal worstRate = null;
        for (Map.Entry<String, int[]> entry : months.entrySet()) {
            BigDecimal rate = AttendanceCalculator.percentage(entry.getValue()[0], entry.getValue()[1], entry.getValue()[3], entry.getValue()[2]);
            if (bestRate == null || rate.compareTo(bestRate) > 0) {
                bestRate = rate;
                best = entry.getKey();
            }
            if (worstRate == null || rate.compareTo(worstRate) < 0) {
                worstRate = rate;
                worst = entry.getKey();
            }
        }
        int training = (int) records.stream().filter(record -> !"MATCH".equalsIgnoreCase(record.getSession().getKind())).count();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("person", person.view());
        body.put("since", records.isEmpty() ? null : records.get(0).getSession().getSessionDate());
        body.put("sessions", records.size());
        body.put("present", counts[0]);
        body.put("late", counts[1]);
        body.put("absent", counts[2]);
        body.put("excused", counts[3]);
        body.put("injured", records.stream().filter(record -> record.getStatus() == AttendanceMark.I).count());
        body.put("notSelected", records.stream().filter(record -> record.getStatus() == AttendanceMark.N).count());
        body.put("rate", AttendanceCalculator.percentage(counts[0], counts[1], counts[3], counts[2]));
        body.put("punctuality", AttendanceCalculator.percentage(counts[0], 0, 0, counts[1] + counts[2] + counts[3]));
        body.put("streak", streak(records, true));
        body.put("longest", streak(records, false));
        body.put("bestMonth", best);
        body.put("worstMonth", worst);
        body.put("finesTotal", issued);
        body.put("finesPaid", paid);
        body.put("finesOutstanding", open);
        body.put("training", training);
        body.put("match", records.size() - training);
        body.put("band", AttendanceCalculator.band(AttendanceCalculator.percentage(counts[0], counts[1], counts[3], counts[2]),
                settingsService.current().getAttendanceOnTrackPct(), settingsService.current().getAttendanceWatchPct()));
        body.put("suspended", person.player != null && person.player.getStatus() == PlayerStatus.SUSPENDED);
        body.put("fines", fines.stream().map(this::fineView).toList());
        body.put("justifications", justificationRepository.findByPersonTypeAndPersonIdOrderByCreatedAtDesc(person.type, person.id).stream().map(this::justView).toList());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> records(String type, Long id) {
        Person person = person(type, id);
        return recordsOf(person).stream()
                .sorted(Comparator.comparing((AttendanceRecord record) -> record.getSession().getSessionDate()).reversed())
                .map(this::recordView)
                .toList();
    }

    public Map<String, Object> justify(AttendanceDeskDtos.JustificationWrite write) {
        if (!canApprove()) {
            throw BusinessException.forbidden("An executive or the assistant manager approves an excuse");
        }
        Person person = person(write.personType(), write.personId());
        AttendanceJustification row = new AttendanceJustification();
        row.setPersonType(person.type);
        row.setPersonId(person.id);
        row.setReason(trim(write.reason(), 1000));
        row.setStatus("APPROVED");
        row.setEvidenceName(trim(write.evidenceName(), 200));
        row.setSubmittedBy(actor());
        if (write.sessionId() != null) {
            TrainingSession session = require(write.sessionId());
            row.setSession(session);
            justificationRepository.save(row);
            AttendanceRecord record = "STAFF".equals(person.type)
                    ? attendanceRepository.findBySessionIdAndStaffId(session.getId(), person.id).orElseGet(AttendanceRecord::new)
                    : attendanceRepository.findBySessionIdAndPlayerId(session.getId(), person.id).orElseGet(AttendanceRecord::new);
            if (record.getId() == null) {
                record.setSession(session);
                record.setPersonType(person.type);
                record.setPlayer(person.player);
                record.setStaff(person.staff);
                record.setRecordedBy(actor());
                record.setRecordedAt(Instant.now(clock));
            }
            AttendanceMark previous = record.getId() == null ? null : record.getStatus();
            record.setStatus(AttendanceMark.E);
            record.setJustified(true);
            record.setNote(trim(write.reason(), 500));
            record.setModifiedBy(actor());
            record.setModifiedAt(Instant.now(clock));
            record.setEditReason(trim(write.reason(), 400));
            attendanceRepository.save(record);
            history(record, previous, AttendanceMark.E, record.getMinutesLate(), write.reason());
            fineService.sync(record, true);
        } else {
            justificationRepository.save(row);
        }
        auditService.record("ATTENDANCE_EXCUSE", person.type, person.id, null, write.reason(), null);
        return justView(row);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> matches() {
        List<FootballMatch> matches = new ArrayList<>(matchRepository.findBySeasonIdOrderByKickoffAsc(settingsService.currentSeason().getId()));
        matches.sort(Comparator.comparing(FootballMatch::getKickoff).reversed());
        if (matches.size() > 40) {
            matches = matches.subList(0, 40);
        }
        return matches.stream().map(match -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", match.getId());
            row.put("label", matchLabel(match));
            return row;
        }).toList();
    }

    @Transactional(readOnly = true)
    public byte[] export(String format, String lang, String audience, String preset, LocalDate from, LocalDate to, String grain,
                         String q, String position, String line, String role, String department, String mark, String kind,
                         String sessionStatus, Boolean hasFine, Boolean edited, String recordedBy) {
        return export(format, lang, audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy, null, "both");
    }

    @Transactional(readOnly = true)
    public byte[] export(String format, String lang, String audience, String preset, LocalDate from, LocalDate to, String grain,
                         String q, String position, String line, String role, String department, String mark, String kind,
                         String sessionStatus, Boolean hasFine, Boolean edited, String recordedBy, String players, String mode) {
        Map<String, Object> grid = grid(audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy, players);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) grid.get("sessions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> people = (List<Map<String, Object>>) grid.get("people");
        List<String> headers = new ArrayList<>(List.of("Name", "Role", "Rate"));
        sessions.forEach(session -> headers.add(String.valueOf(session.get("label"))));
        headers.addAll(List.of("Present", "Late", "Absent", "Excused", "Fines (RWF)"));
        List<Fine> fines = new ArrayList<>();
        for (Map<String, Object> session : sessions) {
            if (session.get("id") instanceof Number number) {
                fines.addAll(fineRepository.findByTrainingSessionId(number.longValue()));
            }
        }
        List<List<String>> csvRows = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> person : people) {
            List<Object> row = new ArrayList<>();
            row.add(String.valueOf(person.get("name")));
            row.add(String.valueOf(person.get("role")));
            row.add(person.get("rate") instanceof Number number ? number : 0);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) person.get("cells");
            BigDecimal fineTotal = BigDecimal.ZERO;
            for (Map<String, Object> session : sessions) {
                Map<String, Object> cell = cells.get(String.valueOf(session.get("id")));
                row.add(cell == null || cell.get("mark") == null ? "" : String.valueOf(cell.get("mark")));
                if (cell != null && cell.get("fine") instanceof BigDecimal amount) {
                    fineTotal = fineTotal.add(amount);
                }
            }
            row.add(person.get("present"));
            row.add(person.get("late"));
            row.add(person.get("absent"));
            row.add(person.get("excused"));
            row.add(fineTotal.longValue());
            rows.add(row);
            List<String> csv = new ArrayList<>();
            for (int column = 0; column < row.size(); column++) {
                Object value = row.get(column);
                csv.add(column == 2 && value instanceof Number rate ? rate + "%"
                        : column == row.size() - 1 ? "RWF " + value
                        : String.valueOf(value));
            }
            csvRows.add(csv);
        }
        String title = "Attendance " + audience(audience).toLowerCase(Locale.ROOT);
        String period = sessions.isEmpty() ? String.valueOf(preset)
                : sessions.get(0).get("date") + " – " + sessions.get(sessions.size() - 1).get("date");
        String chosen = format == null ? "xlsx" : format;
        if ("csv".equals(chosen)) {
            return csv(headers, csvRows);
        }
        return report.general(chosen, lang == null ? "en" : lang, title, actor(), period, headers, rows, fines, mode == null ? "both" : mode);
    }

    @Transactional(readOnly = true)
    public byte[] personExport(String type, Long id, String format, String lang) {
        Map<String, Object> summary = summary(type, id, null, null);
        return report.person(format, lang == null ? "en" : lang, actor(), summary, records(type, id));
    }

    @Transactional(readOnly = true)
    public byte[] template(String audience) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Attendance");
            Row header = sheet.createRow(0);
            String[] titles = "STAFF".equals(audience(audience))
                    ? new String[]{"Name", "Role", "Email", "2026-10-01", "2026-10-02"}
                    : new String[]{"Name", "Jersey", "Email", "2026-10-01", "2026-10-02"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the template");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(MultipartFile file, String audience, String kind) {
        String who = audience(audience);
        assertRead(who);
        List<Person> people = roster(who);
        ClubSettings settings = settingsService.current();
        List<Map<String, Object>> rows = new ArrayList<>();
        int fines = 0;
        for (Parsed parsed : parse(file)) {
            Person match = match(people, parsed);
            Person suggestion = match == null ? suggest(people, parsed.name) : null;
            String error = parsed.error;
            if (error == null && match == null) {
                error = "No matching person";
            }
            if (error == null && parsed.mark == null) {
                error = "Mark must be P, L, A or E";
            }
            FinePolicy.Decision decision = error == null ? decide(kind, parsed.mark, parsed.minutes, settings) : FinePolicy.Decision.none();
            if (decision.fine()) {
                fines++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("line", parsed.line);
            row.put("name", parsed.name);
            row.put("date", parsed.date);
            row.put("status", parsed.mark == null ? "" : parsed.mark.name());
            row.put("minutesLate", parsed.minutes);
            row.put("personId", match == null ? null : match.id);
            row.put("personName", match == null ? "" : match.name);
            row.put("suggestionId", suggestion == null ? null : suggestion.id);
            row.put("suggestion", suggestion == null ? "" : suggestion.name);
            row.put("error", error == null ? "" : error);
            row.put("warning", parsed.mark == AttendanceMark.L && parsed.minutes <= settings.getTrainingLateMinutes() ? "Late mark is within the threshold, so no fine is created" : "");
            row.put("fine", decision.fine() ? decision.amount() : BigDecimal.ZERO);
            row.put("rule", ruleOf(decision.type()));
            rows.add(row);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rows", rows);
        body.put("fines", fines);
        body.put("people", people.stream().map(Person::view).toList());
        return body;
    }

    public Map<String, Object> commit(AttendanceDeskDtos.ImportCommit commit) {
        String who = audience(commit.audience());
        assertWrite(who);
        String kind = commit.kind() == null || commit.kind().isBlank() ? "TRAINING" : commit.kind().toUpperCase(Locale.ROOT);
        Map<LocalDate, List<AttendanceDeskDtos.MarkWrite>> grouped = new LinkedHashMap<>();
        for (AttendanceDeskDtos.ImportRow row : commit.rows()) {
            if (row.personId() == null) {
                continue;
            }
            grouped.computeIfAbsent(row.date(), key -> new ArrayList<>())
                    .add(new AttendanceDeskDtos.MarkWrite(row.personId(), row.status(), row.minutesLate(), null, "Imported"));
        }
        int applied = 0;
        for (Map.Entry<LocalDate, List<AttendanceDeskDtos.MarkWrite>> entry : grouped.entrySet()) {
            TrainingSession session = findSession(entry.getKey(), who, kind);
            if (session == null) {
                session = store(new TrainingSession(), new AttendanceDeskDtos.SessionWrite(kind, who, entry.getKey(), LocalTime.of(16, 0), "", null, "Imported"));
            }
            assertEditable(session, "Imported");
            for (AttendanceDeskDtos.MarkWrite mark : entry.getValue()) {
                applyMark(session, mark, "Imported");
                applied++;
            }
        }
        auditService.record("ATTENDANCE_IMPORT", "TrainingSession", null, null, applied, null);
        return Map.of("applied", applied, "sessions", grouped.size());
    }

    private void applyMark(TrainingSession session, AttendanceDeskDtos.MarkWrite mark, String reason) {
        AttendanceMark status;
        try {
            status = AttendanceMark.valueOf(mark.status().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw BusinessException.badRequest("Unknown mark " + mark.status());
        }
        if (status == AttendanceMark.N && !"MATCH".equalsIgnoreCase(session.getKind())) {
            throw BusinessException.badRequest("Not selected is only used for a match");
        }
        AttendanceRecord record;
        boolean injured = false;
        if ("STAFF".equals(session.getAudience())) {
            StaffMember staff = staffRepository.findById(mark.personId()).orElseThrow(() -> NotFoundException.of("Staff member", mark.personId()));
            if (!AttendanceRoster.trackStaff(staff.getListedIn(), staff.getDepartment(), staff.getRoleName(), staff.isActive())) {
                throw BusinessException.badRequest("This staff member is not on the attendance list");
            }
            record = attendanceRepository.findBySessionIdAndStaffId(session.getId(), staff.getId()).orElseGet(AttendanceRecord::new);
            record.setStaff(staff);
            record.setPlayer(null);
            record.setPersonType("STAFF");
        } else {
            Player player = playerRepository.findById(mark.personId()).orElseThrow(() -> NotFoundException.of("Player", mark.personId()));
            if (!AttendanceRoster.trackPlayer(player.getStatus(), player.isActive())) {
                throw BusinessException.badRequest("This player is not on the active list");
            }
            if (ClubAccess.goalkeepersOnly() && player.getPosition() != Position.GOALKEEPER) {
                throw BusinessException.forbidden("The goalkeeper coach can only mark goalkeepers");
            }
            injured = player.getStatus() == PlayerStatus.INJURED || injured(player.getId(), session.getSessionDate());
            record = attendanceRepository.findBySessionIdAndPlayerId(session.getId(), player.getId()).orElseGet(AttendanceRecord::new);
            record.setPlayer(player);
            record.setStaff(null);
            record.setPersonType("PLAYER");
        }
        if (injured) {
            status = AttendanceMark.I;
        }
        LocalTime arrival = parseTime(mark.arrivalTime());
        int minutes = mark.minutesLate() == null ? 0 : Math.max(0, mark.minutesLate());
        if (arrival != null && session.getStartTime() != null) {
            minutes = (int) Math.max(0, ChronoUnit.MINUTES.between(session.getStartTime(), arrival));
        }
        AttendanceMark previous = record.getId() == null ? null : record.getStatus();
        Integer previousMinutes = record.getMinutesLate();
        boolean changed = record.getId() == null || previous != status || !Objects.equals(previousMinutes, minutes);
        if ("CLOSED".equals(session.getDeskStatus()) && changed && (reason == null || reason.isBlank())) {
            throw BusinessException.unprocessable("A reason is required", "REASON_REQUIRED");
        }
        if (status == AttendanceMark.E && !injured) {
            boolean already = previous == AttendanceMark.E && record.isJustified();
            if (!canApprove() && !already) {
                throw BusinessException.forbidden("An executive or the assistant manager approves an excuse");
            }
            if ((mark.note() == null || mark.note().isBlank()) && !already) {
                throw BusinessException.unprocessable("A reason is required for an excuse", "REASON_REQUIRED");
            }
            record.setJustified(true);
            if (canApprove() && mark.note() != null && !mark.note().isBlank() && previous != AttendanceMark.E) {
                AttendanceJustification excuse = new AttendanceJustification();
                excuse.setPersonType(record.getPersonType());
                excuse.setPersonId(record.getPlayer() != null ? record.getPlayer().getId() : record.getStaff().getId());
                excuse.setSession(session);
                excuse.setReason(trim(mark.note(), 1000));
                excuse.setStatus("APPROVED");
                excuse.setSubmittedBy(actor());
                justificationRepository.save(excuse);
            }
        } else {
            record.setJustified(status == AttendanceMark.E || status == AttendanceMark.I);
        }
        if (record.getId() == null || record.getRecordedBy() == null) {
            record.setRecordedBy(actor());
            record.setRecordedAt(Instant.now(clock));
        } else if (changed) {
            record.setModifiedBy(actor());
            record.setModifiedAt(Instant.now(clock));
            record.setEditReason(trim(reason, 400));
        }
        record.setSession(session);
        record.setStatus(status);
        record.setMinutesLate(minutes);
        record.setArrivalTime(arrival);
        record.setNote(trim(mark.note(), 500));
        attendanceRepository.save(record);
        if (changed) {
            history(record, previous, status, minutes, reason);
        }
        fineService.sync(record, true);
    }

    private TrainingSession store(TrainingSession session, AttendanceDeskDtos.SessionWrite write) {
        ClubSettings settings = settingsService.current();
        String kind = write.kind() == null ? "TRAINING" : write.kind().trim().toUpperCase(Locale.ROOT);
        if (!"MATCH".equals(kind)) {
            kind = "TRAINING";
        }
        session.setSeason(settings.getSeason());
        session.setSessionDate(write.date());
        session.setWeekday(write.date().getDayOfWeek().name());
        session.setWeekNumber(ClubDates.weekNumber(settings.getFirstTrainingMonday(), write.date()));
        session.setSessionType(SessionType.PITCH);
        session.setStartTime(write.startTime() == null ? LocalTime.of(16, 0) : write.startTime());
        session.setLocation(trim(write.place(), 200));
        session.setNotes(trim(write.notes(), 1000));
        session.setAudience(audience(write.audience()));
        session.setKind(kind);
        if (session.getDeskStatus() == null) {
            session.setDeskStatus("OPEN");
        }
        if (write.matchId() != null) {
            session.setMatch(matchRepository.findById(write.matchId()).orElseThrow(() -> NotFoundException.of("Match", write.matchId())));
        } else {
            session.setMatch(null);
        }
        return sessionRepository.save(session);
    }

    private List<Map<String, Object>> rosterRows(TrainingSession session) {
        Map<Long, AttendanceRecord> marked = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRepository.findBySessionId(session.getId())) {
            Long key = "STAFF".equals(record.getPersonType()) && record.getStaff() != null ? record.getStaff().getId()
                    : record.getPlayer() == null ? null : record.getPlayer().getId();
            if (key != null) {
                marked.put(key, record);
            }
        }
        Map<Long, Fine> fines = finesByRecord();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Person person : roster(session.getAudience())) {
            AttendanceRecord record = marked.get(person.id);
            boolean hurt = person.player != null && (person.player.getStatus() == PlayerStatus.INJURED || injured(person.id, session.getSessionDate()));
            Map<String, Object> row = person.view();
            row.put("injured", hurt);
            if (record != null) {
                Fine fine = fines.get(record.getId());
                row.put("status", record.getStatus().name());
                row.put("minutesLate", record.getMinutesLate());
                row.put("arrivalTime", record.getArrivalTime());
                row.put("note", record.getNote());
                row.put("fine", fine == null || fine.getStatus() == FineStatus.WAIVED ? BigDecimal.ZERO : fine.getAmount());
                row.put("rule", fine == null ? "" : ruleOf(fine.getFineType()));
                row.put("recordedBy", record.getRecordedBy());
                row.put("modifiedBy", record.getModifiedBy());
            } else {
                row.put("status", hurt ? "I" : "");
                row.put("minutesLate", 0);
                row.put("fine", BigDecimal.ZERO);
                row.put("rule", "");
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> personRow(Person person, List<TrainingSession> sessions, Map<Long, Fine> fines) {
        Map<Long, AttendanceRecord> bySession = new LinkedHashMap<>();
        for (AttendanceRecord record : recordsOf(person)) {
            bySession.put(record.getSession().getId(), record);
        }
        int present = 0;
        int late = 0;
        int absent = 0;
        int excused = 0;
        Map<String, Map<String, Object>> cells = new LinkedHashMap<>();
        String recordedBy = "";
        Instant recordedAt = null;
        String modifiedBy = "";
        Instant modifiedAt = null;
        for (TrainingSession session : sessions) {
            AttendanceRecord record = bySession.get(session.getId());
            if (record == null) {
                continue;
            }
            switch (record.getStatus()) {
                case P -> present++;
                case L -> late++;
                case A -> absent++;
                case E -> excused++;
                default -> {
                }
            }
            Fine fine = fines.get(record.getId());
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("mark", record.getStatus().name());
            cell.put("minutesLate", record.getMinutesLate());
            cell.put("arrival", record.getArrivalTime());
            cell.put("note", record.getNote());
            cell.put("fine", fine == null || fine.getStatus() == FineStatus.WAIVED ? BigDecimal.ZERO : fine.getAmount());
            cell.put("rule", fine == null ? "" : ruleOf(fine.getFineType()));
            cell.put("recordedBy", record.getRecordedBy());
            cell.put("recordedAt", record.getRecordedAt());
            cell.put("modifiedBy", record.getModifiedBy());
            cell.put("modifiedAt", record.getModifiedAt());
            cells.put(String.valueOf(session.getId()), cell);
            if (record.getRecordedAt() != null && (recordedAt == null || record.getRecordedAt().isAfter(recordedAt))) {
                recordedAt = record.getRecordedAt();
                recordedBy = record.getRecordedBy() == null ? "" : record.getRecordedBy();
            } else if (recordedBy.isBlank() && record.getRecordedBy() != null) {
                recordedBy = record.getRecordedBy();
            }
            if (record.getModifiedAt() != null && (modifiedAt == null || record.getModifiedAt().isAfter(modifiedAt))) {
                modifiedAt = record.getModifiedAt();
                modifiedBy = record.getModifiedBy() == null ? "" : record.getModifiedBy();
            }
        }
        BigDecimal rate = AttendanceCalculator.percentage(present, late, excused, absent);
        Map<String, Object> row = person.view();
        row.put("rate", rate);
        row.put("band", AttendanceCalculator.band(rate, settingsService.current().getAttendanceOnTrackPct(), settingsService.current().getAttendanceWatchPct()));
        row.put("present", present);
        row.put("late", late);
        row.put("absent", absent);
        row.put("excused", excused);
        row.put("counted", present + late + absent + excused);
        row.put("cells", cells);
        row.put("recordedBy", recordedBy);
        row.put("recordedAt", recordedAt);
        row.put("modifiedBy", modifiedBy);
        row.put("modifiedAt", modifiedAt);
        row.put("edited", modifiedAt != null);
        return row;
    }

    private boolean keep(Map<String, Object> row, String q, String position, String line, String role, String department,
                         String mark, Boolean hasFine, Boolean edited, String recordedBy) {
        if (q != null && !q.isBlank() && !String.valueOf(row.get("name")).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!same(position, row.get("position"))) {
            return false;
        }
        if (!same(line, row.get("line"))) {
            return false;
        }
        if (role != null && !role.isBlank() && !"all".equalsIgnoreCase(role)
                && !String.valueOf(row.get("role")).toLowerCase(Locale.ROOT).contains(role.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!same(department, row.get("department"))) {
            return false;
        }
        if (Boolean.TRUE.equals(edited) && !Boolean.TRUE.equals(row.get("edited"))) {
            return false;
        }
        if (recordedBy != null && !recordedBy.isBlank()
                && !String.valueOf(row.get("recordedBy")).toLowerCase(Locale.ROOT).contains(recordedBy.toLowerCase(Locale.ROOT))) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) row.get("cells");
        if (mark != null && !mark.isBlank() && !"all".equalsIgnoreCase(mark)
                && cells.values().stream().noneMatch(cell -> mark.equalsIgnoreCase(String.valueOf(cell.get("mark"))))) {
            return false;
        }
        if (Boolean.TRUE.equals(hasFine)) {
            boolean fined = cells.values().stream().anyMatch(cell -> cell.get("fine") instanceof BigDecimal amount && amount.signum() > 0);
            return fined;
        }
        return true;
    }

    private Map<String, Object> card(TrainingSession session) {
        int present = 0;
        int late = 0;
        int absent = 0;
        int excused = 0;
        String recordedBy = "";
        for (AttendanceRecord record : attendanceRepository.findBySessionId(session.getId())) {
            switch (record.getStatus()) {
                case P -> present++;
                case L -> late++;
                case A -> absent++;
                case E -> excused++;
                default -> {
                }
            }
            if (recordedBy.isBlank() && record.getRecordedBy() != null) {
                recordedBy = record.getRecordedBy();
            }
        }
        List<Fine> fines = fineRepository.findByTrainingSessionId(session.getId()).stream()
                .filter(fine -> fine.getStatus() != FineStatus.WAIVED).toList();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", session.getId());
        row.put("date", session.getSessionDate());
        row.put("kind", session.getKind());
        row.put("audience", session.getAudience());
        row.put("startTime", session.getStartTime());
        row.put("place", session.getLocation());
        row.put("status", session.getDeskStatus());
        row.put("matchId", session.getMatch() == null ? null : session.getMatch().getId());
        row.put("matchLabel", matchLabel(session.getMatch()));
        row.put("present", present);
        row.put("late", late);
        row.put("absent", absent);
        row.put("excused", excused);
        row.put("fines", fines.size());
        row.put("fineTotal", fines.stream().map(Fine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        row.put("recordedBy", recordedBy);
        row.put("closedBy", session.getClosedBy());
        row.put("closedAt", session.getClosedAt());
        row.put("notes", session.getNotes());
        return row;
    }

    private List<Map<String, Object>> columns(List<TrainingSession> sessions) {
        List<Map<String, Object>> columns = sessions.stream().map(this::column).toList();
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Map<String, Object> column : columns) {
            totals.merge(String.valueOf(column.get("label")), 1, Integer::sum);
        }
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<Map<String, Object>> labeled = new ArrayList<>();
        for (Map<String, Object> column : columns) {
            Map<String, Object> copy = new LinkedHashMap<>(column);
            String label = String.valueOf(copy.get("label"));
            if (totals.getOrDefault(label, 1) > 1) {
                int index = seen.merge(label, 1, Integer::sum);
                copy.put("label", label + " · " + index);
            }
            labeled.add(copy);
        }
        return labeled;
    }

    private Map<String, Object> column(TrainingSession session) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", session.getId());
        row.put("date", session.getSessionDate());
        String kindLabel = "MATCH".equalsIgnoreCase(session.getKind()) ? "Match" : "Training";
        String when = session.getSessionDate().getDayOfMonth() + " " + session.getSessionDate().getMonth().name().substring(0, 1)
                + session.getSessionDate().getMonth().name().substring(1, 3).toLowerCase(Locale.ROOT);
        String time = session.getStartTime() == null ? "" : " · " + session.getStartTime().toString().substring(0, 5);
        row.put("label", when + time + " · " + kindLabel);
        row.put("kind", session.getKind());
        row.put("status", session.getDeskStatus());
        row.put("startTime", session.getStartTime());
        return row;
    }

    private List<Map<String, Object>> groups(List<TrainingSession> sessions, String grain) {
        List<Map<String, Object>> groups = new ArrayList<>();
        for (TrainingSession session : sessions) {
            String label = AttendanceRange.bucket(session.getSessionDate(), grain);
            if (!groups.isEmpty() && label.equals(groups.get(groups.size() - 1).get("label"))) {
                groups.get(groups.size() - 1).put("span", ((int) groups.get(groups.size() - 1).get("span")) + 1);
            } else {
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("label", label);
                group.put("span", 1);
                groups.add(group);
            }
        }
        return groups;
    }

    private Map<String, Map<String, Integer>> totals(List<Map<String, Object>> people, List<TrainingSession> sessions) {
        Map<String, Map<String, Integer>> totals = new LinkedHashMap<>();
        for (TrainingSession session : sessions) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("P", 0);
            counts.put("L", 0);
            counts.put("A", 0);
            counts.put("E", 0);
            for (Map<String, Object> person : people) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) person.get("cells");
                Map<String, Object> cell = cells.get(String.valueOf(session.getId()));
                if (cell != null && counts.containsKey(String.valueOf(cell.get("mark")))) {
                    String key = String.valueOf(cell.get("mark"));
                    counts.put(key, counts.get(key) + 1);
                }
            }
            totals.put(String.valueOf(session.getId()), counts);
        }
        return totals;
    }

    private List<TrainingSession> filteredSessions(String audience, LocalDate[] bounds, String kind, String sessionStatus) {
        return seasonSessions().stream()
                .filter(session -> audience.equals(session.getAudience()))
                .filter(session -> AttendanceRange.in(session.getSessionDate(), bounds[0], bounds[1]))
                .filter(session -> kind == null || kind.isBlank() || "all".equalsIgnoreCase(kind) || kind.equalsIgnoreCase(session.getKind()))
                .filter(session -> sessionStatus == null || sessionStatus.isBlank() || "all".equalsIgnoreCase(sessionStatus) || sessionStatus.equalsIgnoreCase(session.getDeskStatus()))
                .toList();
    }

    private List<TrainingSession> seasonSessions() {
        return sessionRepository.findBySeasonIdOrderBySessionDateAsc(settingsService.currentSeason().getId());
    }

    private List<Person> roster(String audience) {
        List<Person> people = new ArrayList<>();
        if ("STAFF".equals(audience)) {
            for (StaffMember staff : staffRepository.findByActiveTrueOrderByFullNameAsc()) {
                if (AttendanceRoster.trackStaff(staff.getListedIn(), staff.getDepartment(), staff.getRoleName(), staff.isActive() && staff.getDeletedAt() == null)) {
                    people.add(Person.staff(staff));
                }
            }
            return people;
        }
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            if (!AttendanceRoster.trackPlayer(player.getStatus(), player.isActive() && player.getDeletedAt() == null)) {
                continue;
            }
            if (ClubAccess.goalkeepersOnly() && player.getPosition() != Position.GOALKEEPER) {
                continue;
            }
            people.add(Person.player(player));
        }
        return people;
    }

    private Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("players", roster("PLAYERS").size());
        counts.put("staff", staffRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .filter(staff -> AttendanceRoster.trackStaff(staff.getListedIn(), staff.getDepartment(), staff.getRoleName(), staff.isActive() && staff.getDeletedAt() == null))
                .count());
        return counts;
    }

    private Map<String, Object> access() {
        Map<String, Object> access = new LinkedHashMap<>();
        access.put("players", canWrite("PLAYERS"));
        access.put("staff", canWrite("STAFF"));
        access.put("closed", canClosed());
        access.put("approve", canApprove());
        return access;
    }

    private Map<String, Object> thresholds() {
        ClubSettings settings = settingsService.current();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lateMinutes", settings.getTrainingLateMinutes());
        row.put("matchLateMinutes", settings.getMatchLateMinutes());
        row.put("lateFine", settings.getTrainingLateFine());
        row.put("absenceFine", settings.getTrainingAbsenceFine());
        row.put("matchLateFine", settings.getMatchLateFine());
        row.put("matchAbsenceFine", settings.getMatchAbsenceFine());
        row.put("onTrack", settings.getAttendanceOnTrackPct());
        row.put("watch", settings.getAttendanceWatchPct());
        return row;
    }

    private boolean injured(Long playerId, LocalDate date) {
        for (Injury injury : injuryRepository.findByStatusNot(InjuryStatus.CLEARED)) {
            if (injury.getPlayer() == null || !playerId.equals(injury.getPlayer().getId())) {
                continue;
            }
            if (injury.getInjuryDate() != null && injury.getInjuryDate().isAfter(date)) {
                continue;
            }
            if (injury.getClearedOn() != null && !injury.getClearedOn().isAfter(date)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private List<AttendanceRecord> recordsOf(Person person) {
        List<AttendanceRecord> records = "STAFF".equals(person.type)
                ? attendanceRepository.findByStaffId(person.id)
                : attendanceRepository.findByPlayerId(person.id);
        return records.stream().filter(record -> person.type.equals(record.getPersonType() == null ? "PLAYER" : record.getPersonType())).toList();
    }

    private Person person(String type, Long id) {
        if ("STAFF".equalsIgnoreCase(type) || "staff".equalsIgnoreCase(type)) {
            StaffMember staff = staffRepository.findById(id).orElseThrow(() -> NotFoundException.of("Staff member", id));
            assertRead("STAFF");
            return Person.staff(staff);
        }
        Player player = playerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Player", id));
        assertRead("PLAYERS");
        return Person.player(player);
    }

    private List<Fine> finesOf(Person person) {
        return "STAFF".equals(person.type)
                ? fineRepository.findByStaffIdOrderByFineDateAsc(person.id)
                : fineRepository.findByPlayerIdOrderByFineDateAsc(person.id);
    }

    private Map<Long, Fine> finesByRecord() {
        Map<Long, Fine> fines = new LinkedHashMap<>();
        for (Fine fine : fineRepository.findAll()) {
            if (fine.getAttendanceRecord() != null) {
                fines.put(fine.getAttendanceRecord().getId(), fine);
            }
        }
        return fines;
    }

    private void history(AttendanceRecord record, AttendanceMark previous, AttendanceMark next, Integer minutes, String reason) {
        AttendanceRecordHistory row = new AttendanceRecordHistory();
        row.setRecord(record);
        row.setOldStatus(previous == null ? null : previous.name());
        row.setNewStatus(next.name());
        row.setMinutesLate(minutes);
        row.setReason(trim(reason, 400));
        row.setChangedBy(actor());
        row.setChangedAt(Instant.now(clock));
        historyRepository.save(row);
    }

    private Map<String, Object> recordView(AttendanceRecord record) {
        Fine fine = fineRepository.findByAttendanceRecordId(record.getId()).orElse(null);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", record.getId());
        row.put("sessionId", record.getSession().getId());
        row.put("date", record.getSession().getSessionDate());
        row.put("kind", record.getSession().getKind());
        row.put("status", record.getStatus().name());
        row.put("arrival", record.getArrivalTime());
        row.put("minutesLate", record.getMinutesLate());
        row.put("fine", fine == null || fine.getStatus() == FineStatus.WAIVED ? BigDecimal.ZERO : fine.getAmount());
        row.put("rule", fine == null ? "" : ruleOf(fine.getFineType()));
        row.put("note", record.getNote());
        row.put("recordedBy", record.getRecordedBy());
        row.put("recordedAt", record.getRecordedAt());
        row.put("modifiedBy", record.getModifiedBy());
        row.put("modifiedAt", record.getModifiedAt());
        row.put("history", historyRepository.findByRecord_IdOrderByChangedAtAsc(record.getId()).stream().map(item -> {
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("oldStatus", item.getOldStatus());
            change.put("newStatus", item.getNewStatus());
            change.put("who", item.getChangedBy());
            change.put("when", item.getChangedAt());
            change.put("reason", item.getReason());
            return change;
        }).toList());
        return row;
    }

    private Map<String, Object> fineView(Fine fine) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", fine.getId());
        row.put("date", fine.getFineDate());
        row.put("amount", fine.getAmount());
        row.put("paid", fine.getAmountPaid());
        row.put("status", fine.getStatus().name());
        row.put("rule", ruleOf(fine.getFineType()));
        row.put("sessionId", fine.getTrainingSession() == null ? null : fine.getTrainingSession().getId());
        String source = fine.getTrainingSession() != null ? "Training " + fine.getTrainingSession().getSessionDate()
                : fine.getMatch() != null && fine.getMatch().getOpponent() != null ? "Match vs " + fine.getMatch().getOpponent().getName()
                : "Manual";
        row.put("source", source);
        return row;
    }

    private Map<String, Object> justView(AttendanceJustification row) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", row.getId());
        body.put("reason", row.getReason());
        body.put("status", row.getStatus());
        body.put("evidenceName", row.getEvidenceName());
        body.put("submittedBy", row.getSubmittedBy());
        body.put("sessionId", row.getSession() == null ? null : row.getSession().getId());
        body.put("date", row.getSession() == null ? null : row.getSession().getSessionDate());
        return body;
    }

    private Map<String, Object> page(List<Map<String, Object>> items, int page, int size, Map<String, Object> counts) {
        int safeSize = size < 1 ? 50 : Math.min(size, 200);
        int safePage = Math.max(page, 0);
        int start = Math.min(safePage * safeSize, items.size());
        int end = Math.min(start + safeSize, items.size());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items.subList(start, end));
        body.put("total", items.size());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("counts", counts);
        body.put("access", access());
        return body;
    }

    private List<Map<String, Object>> groupRates(List<Map<String, Object>> people, String field) {
        Map<String, int[]> grouped = new LinkedHashMap<>();
        Map<String, Integer> heads = new LinkedHashMap<>();
        for (Map<String, Object> person : people) {
            String name = String.valueOf(person.get(field) == null ? "" : person.get(field));
            if (name.isBlank()) {
                continue;
            }
            int[] counts = grouped.computeIfAbsent(name, key -> new int[4]);
            counts[0] += ((Number) person.get("present")).intValue();
            counts[1] += ((Number) person.get("late")).intValue();
            counts[2] += ((Number) person.get("absent")).intValue();
            counts[3] += ((Number) person.get("excused")).intValue();
            heads.merge(name, 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.forEach((name, counts) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("count", heads.get(name));
            row.put("rate", AttendanceCalculator.percentage(counts[0], counts[1], counts[3], counts[2]));
            rows.add(row);
        });
        return rows;
    }

    private List<Map<String, Object>> heatmap(List<Map<String, Object>> people, List<Map<String, Object>> sessions) {
        Map<String, int[]> cells = new LinkedHashMap<>();
        for (Map<String, Object> session : sessions) {
            LocalDate date = LocalDate.parse(String.valueOf(session.get("date")));
            int hour = 16;
            if (session.get("startTime") != null && !String.valueOf(session.get("startTime")).isBlank()) {
                hour = LocalTime.parse(String.valueOf(session.get("startTime")).substring(0, 5)).getHour();
            }
            String key = date.getDayOfWeek().name() + "|" + hour;
            int[] counts = cells.computeIfAbsent(key, ignored -> new int[2]);
            for (Map<String, Object> person : people) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> marks = (Map<String, Map<String, Object>>) person.get("cells");
                Map<String, Object> cell = marks.get(String.valueOf(session.get("id")));
                if (cell == null || cell.get("mark") == null || String.valueOf(cell.get("mark")).isBlank()) {
                    continue;
                }
                counts[1]++;
                if ("P".equals(cell.get("mark")) || "L".equals(cell.get("mark"))) {
                    counts[0]++;
                }
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        cells.forEach((key, counts) -> {
            String[] parts = key.split("\\|");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("weekday", parts[0]);
            row.put("hour", Integer.parseInt(parts[1]));
            row.put("present", counts[0]);
            row.put("total", counts[1]);
            rows.add(row);
        });
        return rows;
    }

    private List<Map<String, Object>> compare(String compare, List<Map<String, Object>> people, List<Map<String, Object>> sessions, String grain) {
        if (compare == null || compare.isBlank()) {
            return List.of();
        }
        List<String> ids = List.of(compare.split(","));
        List<Map<String, Object>> series = new ArrayList<>();
        for (Map<String, Object> person : people) {
            if (!ids.contains(String.valueOf(person.get("personId")))) {
                continue;
            }
            if (series.size() == 5) {
                break;
            }
            Map<String, int[]> buckets = new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) person.get("cells");
            for (Map<String, Object> session : sessions) {
                Map<String, Object> cell = cells.get(String.valueOf(session.get("id")));
                if (cell == null || cell.get("mark") == null) {
                    continue;
                }
                String label = AttendanceRange.bucket(LocalDate.parse(String.valueOf(session.get("date"))), grain);
                int[] counts = buckets.computeIfAbsent(label, key -> new int[4]);
                switch (String.valueOf(cell.get("mark"))) {
                    case "P" -> counts[0]++;
                    case "L" -> counts[1]++;
                    case "A" -> counts[2]++;
                    case "E" -> counts[3]++;
                    default -> {
                    }
                }
            }
            List<Map<String, Object>> points = new ArrayList<>();
            buckets.forEach((label, counts) -> points.add(Map.of("label", label, "rate", AttendanceCalculator.percentage(counts[0], counts[1], counts[3], counts[2]))));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("personId", person.get("personId"));
            row.put("name", person.get("name"));
            row.put("points", points);
            series.add(row);
        }
        return series;
    }

    private Map<String, Object> rank(Map<String, Object> person) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("personId", person.get("personId"));
        row.put("personType", person.get("personType"));
        row.put("name", person.get("name"));
        row.put("photoUrl", person.get("photoUrl"));
        row.put("rate", person.get("rate"));
        row.put("role", person.get("role"));
        return row;
    }

    private Map<String, Object> slice(String key, int value) {
        return Map.of("key", key, "value", value);
    }

    private TrainingSession findSession(LocalDate date, String audience, String kind) {
        return seasonSessions().stream()
                .filter(session -> date.equals(session.getSessionDate()))
                .filter(session -> audience.equals(session.getAudience()))
                .filter(session -> kind.equalsIgnoreCase(session.getKind()))
                .min(Comparator.comparing(session -> "OPEN".equals(session.getDeskStatus()) ? 0 : 1))
                .orElse(null);
    }

    private TrainingSession require(Long id) {
        return sessionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Training session", id));
    }

    private void assertEditable(TrainingSession session, String reason) {
        if (!"CLOSED".equals(session.getDeskStatus())) {
            return;
        }
        assertClosedEditor();
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reason is required", "REASON_REQUIRED");
        }
    }

    private void assertRead(String audience) {
        if ("STAFF".equals(audience) && !canWrite("STAFF")) {
            throw BusinessException.forbidden("Staff attendance is recorded by the assistant manager and executives");
        }
        if (!canWrite("PLAYERS") && !canWrite("STAFF")) {
            throw BusinessException.forbidden("You cannot view attendance");
        }
    }

    private void assertWrite(String audience) {
        if (!canWrite(audience)) {
            throw BusinessException.forbidden("STAFF".equals(audience)
                    ? "Staff attendance is recorded by the assistant manager and executives"
                    : "You cannot record this attendance");
        }
    }

    private void assertClosedEditor() {
        if (!canClosed()) {
            throw BusinessException.forbidden("Only an assistant manager or an executive can change a closed session");
        }
    }

    private boolean canWrite(String audience) {
        if (SecurityUtils.hasRole("DOCTOR") && !SecurityUtils.isExecutive()) {
            return false;
        }
        if ("STAFF".equals(audience)) {
            return SecurityUtils.isExecutive() || SecurityUtils.hasRole("ASSISTANT_MANAGER");
        }
        return SecurityUtils.isExecutive()
                || SecurityUtils.hasRole("ASSISTANT_MANAGER")
                || SecurityUtils.hasRole("HEAD_COACH")
                || SecurityUtils.hasRole("ASSISTANT_COACH")
                || SecurityUtils.hasRole("GOALKEEPER_COACH");
    }

    private boolean canClosed() {
        return SecurityUtils.isExecutive() || SecurityUtils.hasRole("ASSISTANT_MANAGER");
    }

    private boolean canApprove() {
        return canClosed();
    }

    private String actor() {
        var principal = SecurityUtils.principal();
        String name = principal != null && principal.getFullName() != null && !principal.getFullName().isBlank()
                ? principal.getFullName() : SecurityUtils.email();
        return name.length() > 120 ? name.substring(0, 120) : name;
    }

    private String audience(String raw) {
        return raw != null && raw.equalsIgnoreCase("staff") || "STAFF".equalsIgnoreCase(raw) ? "STAFF" : "PLAYERS";
    }

    private boolean same(String filter, Object value) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        return norm(filter).equals(norm(String.valueOf(value)));
    }

    private String norm(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "").replace(" ", "");
    }

    private String matchLabel(FootballMatch match) {
        if (match == null) {
            return "";
        }
        String opponent = match.getOpponent() == null ? "Match" : match.getOpponent().getName();
        return opponent + " · " + match.getKickoff().atZone(KIGALI).toLocalDate();
    }

    private String ruleOf(FineType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case TRAINING_LATE -> "6";
            case TRAINING_ABSENCE -> "7";
            case MATCH_LATE, MATCH_ABSENCE -> "8";
            default -> "";
        };
    }

    private FinePolicy.Decision decide(String kind, AttendanceMark mark, int minutes, ClubSettings settings) {
        if (mark == null) {
            return FinePolicy.Decision.none();
        }
        if ("MATCH".equalsIgnoreCase(kind)) {
            return FinePolicy.matchMark(mark, minutes, mark == AttendanceMark.E, settings.getMatchLateMinutes(), settings.getMatchLateFine(), settings.getMatchAbsenceFine());
        }
        return FinePolicy.training(mark, minutes, mark == AttendanceMark.E, settings.getTrainingLateMinutes(), settings.getTrainingLateFine(), settings.getTrainingAbsenceFine());
    }

    private int[] count(List<AttendanceRecord> records) {
        int[] counts = new int[4];
        for (AttendanceRecord record : records) {
            add(counts, record.getStatus());
        }
        return counts;
    }

    private void add(int[] counts, AttendanceMark status) {
        switch (status) {
            case P -> counts[0]++;
            case L -> counts[1]++;
            case A -> counts[2]++;
            case E -> counts[3]++;
            default -> {
            }
        }
    }

    private int streak(List<AttendanceRecord> records, boolean currentOnly) {
        int longest = 0;
        int run = 0;
        for (AttendanceRecord record : records) {
            if (record.getStatus() == AttendanceMark.I || record.getStatus() == AttendanceMark.N) {
                continue;
            }
            if (record.getStatus() == AttendanceMark.P || record.getStatus() == AttendanceMark.L) {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 0;
            }
        }
        return currentOnly ? run : longest;
    }

    private String append(String notes, String extra) {
        String base = notes == null ? "" : notes;
        String joined = base.isBlank() ? extra : base + "\n" + extra;
        return trim(joined, 1000);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.length() >= 5 ? value.substring(0, 5) : value;
        return LocalTime.parse(text);
    }

    private byte[] csv(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers.stream().map(this::cell).toList())).append('\n');
        for (List<String> row : rows) {
            csv.append(String.join(",", row.stream().map(this::cell).toList())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String cell(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private List<Parsed> parse(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                String text = new String(file.getBytes(), StandardCharsets.UTF_8);
                return parseTable(text.lines().map(line -> List.of(line.split(",", -1))).toList());
            }
            try (InputStream input = file.getInputStream(); Workbook book = WorkbookFactory.create(input)) {
                Sheet sheet = book.getSheetAt(0);
                List<List<String>> table = new ArrayList<>();
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }
                    List<String> values = new ArrayList<>();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                    }
                    table.add(values);
                }
                return parseTable(table);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("Could not read the file");
        }
    }

    private List<Parsed> parseTable(List<List<String>> table) {
        if (table.isEmpty()) {
            return List.of();
        }
        List<String> header = table.get(0).stream().map(value -> value.replace("\"", "").trim()).toList();
        List<Integer> dates = new ArrayList<>();
        List<LocalDate> dateValues = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            LocalDate date = dateOf(header.get(i));
            if (date != null) {
                dates.add(i);
                dateValues.add(date);
            }
        }
        List<Parsed> rows = new ArrayList<>();
        if (!dates.isEmpty()) {
            int nameCol = index(header, "name", "player", "staff");
            if (nameCol < 0) {
                nameCol = 0;
            }
            for (int r = 1; r < table.size(); r++) {
                List<String> line = table.get(r);
                String person = at(line, nameCol);
                if (person.isBlank()) {
                    continue;
                }
                for (int i = 0; i < dates.size(); i++) {
                    String mark = at(line, dates.get(i));
                    if (mark.isBlank()) {
                        continue;
                    }
                    rows.add(new Parsed(r + 1, person, dateValues.get(i), markOf(mark), 0, null));
                }
            }
            return rows;
        }
        int nameCol = index(header, "name", "player", "staff");
        int dateCol = index(header, "date");
        int markCol = index(header, "status", "mark");
        int lateCol = index(header, "minutes", "minutes late");
        for (int r = 1; r < table.size(); r++) {
            List<String> line = table.get(r);
            String person = at(line, nameCol);
            if (person.isBlank()) {
                continue;
            }
            String late = at(line, lateCol);
            int minutes = late.isBlank() ? 0 : Integer.parseInt(late.replaceAll("[^0-9]", ""));
            rows.add(new Parsed(r + 1, person, dateOf(at(line, dateCol)), markOf(at(line, markCol)), minutes, dateOf(at(line, dateCol)) == null ? "Date is missing" : null));
        }
        return rows;
    }

    private Person match(List<Person> people, Parsed parsed) {
        for (Person person : people) {
            if (person.name.equalsIgnoreCase(parsed.name) || (person.known != null && person.known.equalsIgnoreCase(parsed.name))
                    || (person.email != null && person.email.equalsIgnoreCase(parsed.name))
                    || (person.jersey != null && person.jersey.equalsIgnoreCase(parsed.name))) {
                return person;
            }
        }
        return null;
    }

    private Person suggest(List<Person> people, String name) {
        Person best = null;
        int score = 3;
        for (Person person : people) {
            int distance = Math.min(distance(name, person.name), person.known == null ? 99 : distance(name, person.known));
            if (distance < score) {
                score = distance;
                best = person;
            }
        }
        return best;
    }

    private int distance(String left, String right) {
        String a = left == null ? "" : left.toLowerCase(Locale.ROOT);
        String b = right == null ? "" : right.toLowerCase(Locale.ROOT);
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private int index(List<String> header, String... names) {
        for (int i = 0; i < header.size(); i++) {
            String value = header.get(i).toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (value.equals(name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String at(List<String> line, int index) {
        if (index < 0 || index >= line.size() || line.get(index) == null) {
            return "";
        }
        return line.get(index).replace("\"", "").trim();
    }

    private LocalDate dateOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() >= 10 && text.charAt(4) == '-') {
                return LocalDate.parse(text.substring(0, 10));
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private AttendanceMark markOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().toUpperCase(Locale.ROOT);
        return switch (text) {
            case "P", "PRESENT" -> AttendanceMark.P;
            case "L", "LATE" -> AttendanceMark.L;
            case "A", "ABSENT" -> AttendanceMark.A;
            case "E", "EXCUSED" -> AttendanceMark.E;
            case "I", "INJURED" -> AttendanceMark.I;
            case "N", "NOT SELECTED" -> AttendanceMark.N;
            default -> null;
        };
    }

    private record Parsed(int line, String name, LocalDate date, AttendanceMark mark, int minutes, String error) {
    }

    private static final class Person {
        private final String type;
        private final Long id;
        private final String name;
        private final String known;
        private final String email;
        private final String jersey;
        private final String role;
        private final String position;
        private final String line;
        private final String department;
        private final String photoUrl;
        private final Player player;
        private final StaffMember staff;

        private Person(String type, Long id, String name, String known, String email, String jersey, String role, String position,
                       String line, String department, String photoUrl, Player player, StaffMember staff) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.known = known;
            this.email = email;
            this.jersey = jersey;
            this.role = role;
            this.position = position;
            this.line = line;
            this.department = department;
            this.photoUrl = photoUrl;
            this.player = player;
            this.staff = staff;
        }

        private static Person player(Player player) {
            return new Person("PLAYER", player.getId(), player.getFullName(), player.getKnownAs(), player.getEmail(),
                    player.getJerseyNumber() == null ? "" : String.valueOf(player.getJerseyNumber()),
                    player.getPosition().getLabel(), player.getPosition().getLabel(), player.getLineName(), "",
                    player.getPhotoUrl(), player, null);
        }

        private static Person staff(StaffMember staff) {
            return new Person("STAFF", staff.getId(), staff.getFullName(), staff.getFullName(), staff.getEmail(), "",
                    staff.getRoleName(), staff.getRoleName(), "", staff.getDepartment(), staff.getPhotoUrl(), null, staff);
        }

        private Map<String, Object> view() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("personType", type);
            row.put("personId", id);
            row.put("name", name);
            row.put("knownAs", known);
            row.put("email", email);
            row.put("jersey", jersey);
            row.put("role", role);
            row.put("position", position);
            row.put("line", line);
            row.put("department", department);
            row.put("photoUrl", photoUrl);
            return row;
        }
    }
}
