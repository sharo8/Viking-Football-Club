package rw.vikingfc.club.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.sanction.FineService;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.training.TrainingSession;
import rw.vikingfc.club.training.TrainingSessionRepository;
import rw.vikingfc.club.user.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final TrainingSessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;
    private final FineService fineService;
    private final NotificationService notificationService;

    public List<Map<String, Object>> saveBulk(Long sessionId, List<AttendanceDtos.Mark> marks) {
        TrainingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> NotFoundException.of("Training session", sessionId));
        if ("CLOSED".equals(session.getDeskStatus())) {
            throw BusinessException.forbidden("This session is closed");
        }
        List<String> atRisk = new ArrayList<>();
        for (AttendanceDtos.Mark mark : marks) {
            Player player = playerRepository.findByIdAndActiveTrue(mark.playerId())
                    .orElseThrow(() -> NotFoundException.of("Player", mark.playerId()));
            if (ClubAccess.goalkeepersOnly() && player.getPosition() != Position.GOALKEEPER) {
                throw BusinessException.forbidden("The goalkeeper coach can only mark goalkeepers");
            }
            AttendanceRecord record = attendanceRepository.findBySessionIdAndPlayerId(sessionId, player.getId())
                    .orElseGet(AttendanceRecord::new);
            record.setSession(session);
            record.setPlayer(player);
            record.setStatus(mark.status());
            record.setMinutesLate(mark.minutesLate() == null ? 0 : mark.minutesLate());
            record.setJustified(Boolean.TRUE.equals(mark.justified()) || mark.status() == AttendanceMark.E);
            record.setNote(mark.note());
            record.setRecordedBy(SecurityUtils.email());
            attendanceRepository.save(record);
            fineService.sync(record, true);
        }
        ClubSettings settings = settingsService.current();
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            Stats stats = stats(attendanceRepository.findByPlayerId(player.getId()));
            String band = AttendanceCalculator.band(stats.percent, settings.getAttendanceOnTrackPct(), settings.getAttendanceWatchPct());
            if ("At risk".equals(band)) {
                atRisk.add(player.getKnownAs());
            }
        }
        if (!atRisk.isEmpty()) {
            notificationService.notifyRoles(List.of(Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.GOALKEEPER_COACH, Role.ASSISTANT_MANAGER),
                    "ATTENDANCE", "Players at risk", String.join(", ", atRisk), "/attendance");
        }
        return sessionView(sessionId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> kpis(Integer week, String weekday, LocalDate date, String position, Long playerId) {
        List<AttendanceRecord> rows = filtered(week, weekday, date, position, playerId);
        Stats stats = stats(rows);
        ClubSettings settings = settingsService.current();
        long sessions = rows.stream().map(row -> row.getSession().getId()).distinct().count();
        long players = rows.stream().map(row -> row.getPlayer().getId()).distinct().count();
        BigDecimal absenceFines = BigDecimal.valueOf(stats.absent).multiply(settings.getTrainingAbsenceFine());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sessions", sessions);
        body.put("players", players);
        body.put("present", stats.present + stats.late);
        body.put("permission", stats.excused);
        body.put("absent", stats.absent);
        body.put("late", stats.late);
        body.put("attendancePercent", stats.percent);
        body.put("band", AttendanceCalculator.band(stats.percent, settings.getAttendanceOnTrackPct(), settings.getAttendanceWatchPct()));
        body.put("absenceFines", absenceFines);
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> atRisk() {
        ClubSettings settings = settingsService.current();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            if (ClubAccess.goalkeepersOnly() && player.getPosition() != Position.GOALKEEPER) {
                continue;
            }
            Stats stats = stats(attendanceRepository.findByPlayerId(player.getId()));
            String band = AttendanceCalculator.band(stats.percent, settings.getAttendanceOnTrackPct(), settings.getAttendanceWatchPct());
            if ("At risk".equals(band)) {
                rows.add(Map.of("playerId", player.getId(), "name", player.getFullName(), "knownAs", player.getKnownAs(),
                        "position", player.getPosition().getLabel(), "attendancePercent", stats.percent, "band", band));
            }
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> matrix() {
        List<TrainingSession> sessions = sessionRepository.findBySeasonIdOrderBySessionDateAsc(settingsService.currentSeason().getId());
        if (sessions.size() > 12) {
            sessions = sessions.subList(sessions.size() - 12, sessions.size());
        }
        List<Map<String, Object>> players = new ArrayList<>();
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            if (ClubAccess.goalkeepersOnly() && player.getPosition() != Position.GOALKEEPER) {
                continue;
            }
            List<AttendanceRecord> records = attendanceRepository.findByPlayerId(player.getId());
            Stats stats = stats(records);
            Map<String, String> cells = new LinkedHashMap<>();
            Map<String, String> recorded = new LinkedHashMap<>();
            for (TrainingSession session : sessions) {
                AttendanceRecord match = records.stream()
                        .filter(record -> record.getSession().getId().equals(session.getId()))
                        .findFirst().orElse(null);
                String day = session.getSessionDate().toString();
                cells.put(day, match == null ? "" : match.getStatus().name());
                recorded.put(day, match == null || match.getRecordedBy() == null ? "" : match.getRecordedBy());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", player.getId());
            row.put("name", player.getFullName());
            row.put("knownAs", player.getKnownAs());
            row.put("position", player.getPosition().getLabel());
            row.put("cells", cells);
            row.put("recorded", recorded);
            row.put("present", stats.present + stats.late);
            row.put("permission", stats.excused);
            row.put("absent", stats.absent);
            row.put("late", stats.late);
            row.put("attendancePercent", stats.percent);
            players.add(row);
        }
        return Map.of("sessions", sessions.stream().map(TrainingSession::getSessionDate).toList(), "players", players);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> weekly() {
        Map<Integer, List<AttendanceRecord>> grouped = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRepository.findBySession_Season_Id(settingsService.currentSeason().getId())) {
            if (record.getPlayer() == null) {
                continue;
            }
            grouped.computeIfAbsent(record.getSession().getWeekNumber(), key -> new ArrayList<>()).add(record);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Stats stats = stats(entry.getValue());
            long sessions = entry.getValue().stream().map(record -> record.getSession().getId()).distinct().count();
            List<String> absent = entry.getValue().stream().filter(record -> record.getStatus() == AttendanceMark.A)
                    .map(record -> record.getPlayer().getKnownAs()).distinct().sorted().toList();
            List<String> permission = entry.getValue().stream().filter(record -> record.getStatus() == AttendanceMark.E)
                    .map(record -> record.getPlayer().getKnownAs()).distinct().sorted().toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("week", entry.getKey());
            row.put("sessions", sessions);
            row.put("present", stats.present + stats.late);
            row.put("permission", stats.excused);
            row.put("absent", stats.absent);
            row.put("attendancePercent", stats.percent);
            row.put("averagePresentPerSession", sessions == 0 ? 0 : (stats.present + stats.late) / sessions);
            row.put("unexcusedAbsence", absent);
            row.put("onPermission", permission);
            rows.add(row);
        });
        return rows;
    }

    private List<Map<String, Object>> sessionView(Long sessionId) {
        return attendanceRepository.findBySessionId(sessionId).stream()
                .filter(record -> record.getPlayer() != null)
                .sorted(Comparator.comparing(record -> record.getPlayer().getRegisterNo()))
                .map(record -> Map.<String, Object>of(
                        "playerId", record.getPlayer().getId(),
                        "knownAs", record.getPlayer().getKnownAs(),
                        "status", record.getStatus()))
                .toList();
    }

    private List<AttendanceRecord> filtered(Integer week, String weekday, LocalDate date, String position, Long playerId) {
        return attendanceRepository.findBySession_Season_Id(settingsService.currentSeason().getId()).stream()
                .filter(record -> record.getPlayer() != null)
                .filter(record -> week == null || record.getSession().getWeekNumber() == week)
                .filter(record -> weekday == null || weekday.equalsIgnoreCase("All") || record.getSession().getWeekday().equalsIgnoreCase(weekday))
                .filter(record -> date == null || record.getSession().getSessionDate().equals(date))
                .filter(record -> position == null || position.equalsIgnoreCase("All") || record.getPlayer().getPosition().getLabel().equalsIgnoreCase(position)
                        || record.getPlayer().getPosition().name().equalsIgnoreCase(position))
                .filter(record -> playerId == null || record.getPlayer().getId().equals(playerId))
                .filter(record -> !ClubAccess.goalkeepersOnly() || record.getPlayer().getPosition() == Position.GOALKEEPER)
                .toList();
    }

    private Stats stats(List<AttendanceRecord> rows) {
        Stats stats = new Stats();
        for (AttendanceRecord row : rows) {
            switch (row.getStatus()) {
                case P -> stats.present++;
                case L -> stats.late++;
                case E -> stats.excused++;
                case A -> stats.absent++;
                case I, N -> {
                }
            }
        }
        stats.percent = AttendanceCalculator.percentage(stats.present, stats.late, stats.excused, stats.absent);
        return stats;
    }

    private static final class Stats {
        private int present;
        private int late;
        private int excused;
        private int absent;
        private BigDecimal percent = BigDecimal.ZERO;
    }
}
