package rw.vikingfc.club.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceService;
import rw.vikingfc.club.audit.AuditViewService;
import rw.vikingfc.club.bonus.BonusEarned;
import rw.vikingfc.club.bonus.BonusEarnedRepository;
import rw.vikingfc.club.bonus.BonusStatus;
import rw.vikingfc.club.common.api.PageResponse;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.finance.FinanceDtos;
import rw.vikingfc.club.finance.FinanceService;
import rw.vikingfc.club.match.EventType;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.match.MatchEvent;
import rw.vikingfc.club.match.MatchEventRepository;
import rw.vikingfc.club.match.MatchPlayerRecord;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.medical.MedicalService;
import rw.vikingfc.club.partner.PartnerService;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.sanction.FineService;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.staff.StaffMemberRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubHomeService {

    private static final Set<String> FINANCE_CHARTS = Set.of(
            "payroll", "salaryBands", "payment", "cashout", "earners", "bonuses");
    private static final Set<String> MEDICAL_CHARTS = Set.of("injuryParts", "injured");
    private static final Set<String> ATTENDANCE_CHARTS = Set.of("attendance", "attendanceStatus");
    private static final Set<String> PARTNER_CHARTS = Set.of("partners");

    private final FootballMatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final PlayerRepository playerRepository;
    private final StaffMemberRepository staffRepository;
    private final PlayerContractRepository contractRepository;
    private final AuditViewService auditViewService;
    private final BonusEarnedRepository bonusRepository;
    private final FinanceService financeService;
    private final AttendanceService attendanceService;
    private final FineService fineService;
    private final MedicalService medicalService;
    private final PartnerService partnerService;
    private final SettingsService settingsService;
    private final Clock clock;

    public Map<String, Object> summary(String period) {
        List<FootballMatch> current = finished(period);
        List<FootballMatch> previous = previousFinished(period);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("players", playerRepository.countByActiveTrue());
        body.put("staff", staffRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .filter(staff -> staff.getListedIn() == null || !"ADMINISTRATION".equalsIgnoreCase(staff.getListedIn()))
                .count());
        body.put("played", current.size());
        body.put("goalsFor", sum(current, FootballMatch::getGoalsFor));
        body.put("goalsAgainst", sum(current, FootballMatch::getGoalsAgainst));
        body.put("winRate", rate(wins(current), current.size()));
        body.put("attendanceRate", attendanceRate());
        body.put("injured", openInjuries().size());
        if (executive()) {
            FinanceDtos.Summary money = safeFinance();
            if (money != null) {
                body.put("payroll", money.seasonPayroll());
                body.put("paidPct", money.paidPercent());
                body.put("balance", money.netBalanceDue());
            }
            Object outstanding = fineService.summary().get("outstanding");
            body.put("fines", outstanding);
        }
        body.put("trends", trends(current, previous));
        body.put("sparks", sparks(current));
        return body;
    }

    public Map<String, Object> nextMatch() {
        Instant now = Instant.now(clock);
        return seasonMatches().stream()
                .filter(match -> match.getStatus() != MatchStatus.FINISHED && match.getStatus() != MatchStatus.CANCELLED)
                .filter(match -> !match.getKickoff().isBefore(now) || match.getStatus() == MatchStatus.LIVE)
                .min(Comparator.comparing(FootballMatch::getKickoff))
                .map(this::fixture)
                .orElse(null);
    }

    public PageResponse<Map<String, Object>> recentMatches(String period, int page, int size) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<FootballMatch> selected = new ArrayList<>(finished(period));
        selected.sort(Comparator.comparing(FootballMatch::getKickoff).reversed());
        for (FootballMatch match : selected) {
            rows.add(playedCard(match));
        }
        return page(rows, page, size);
    }

    public PageResponse<Map<String, Object>> topScorers(String scope, int page, int size) {
        return page(rank("goals", scope), page, size);
    }

    public PageResponse<Map<String, Object>> topAssists(String scope, int page, int size) {
        return page(rank("assists", scope), page, size);
    }

    public PageResponse<Map<String, Object>> recentGoals(String period, int page, int size) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<FootballMatch> selected = new ArrayList<>(finished(period));
        selected.sort(Comparator.comparing(FootballMatch::getKickoff).reversed());
        for (FootballMatch match : selected) {
            List<MatchEvent> events = eventRepository.findByMatchIdOrderByEventMinuteAsc(match.getId());
            events.stream()
                    .filter(event -> event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL)
                    .sorted(Comparator.comparingInt(MatchEvent::getEventMinute).reversed())
                    .forEach(event -> rows.add(goalRow(match, event)));
        }
        return page(rows, page, size);
    }

    public PageResponse<Map<String, Object>> activity(int page, int size) {
        return auditViewService.list(page, size, null, null);
    }

    public List<Map<String, Object>> alerts() {
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate horizon = LocalDate.now(clock).plusMonths(6);
        Long seasonId = seasonId();
        if (seasonId != null && (sport() || people())) {
            for (PlayerContract contract : contractRepository.findBySeasonIdAndActiveTrue(seasonId)) {
                if (!contract.getEndDate().isAfter(horizon)) {
                    rows.add(alert("c-" + contract.getId(), "wait", "alertContract", contract.getPlayer().getKnownAs(), contract.getEndDate().toString()));
                }
            }
            Map<Integer, List<String>> dupes = new LinkedHashMap<>();
            for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
                if (player.getJerseyNumber() != null && playerRepository.findDuplicateJerseys().contains(player.getJerseyNumber())) {
                    dupes.computeIfAbsent(player.getJerseyNumber(), key -> new ArrayList<>()).add(player.getKnownAs());
                }
            }
            dupes.forEach((jersey, names) -> rows.add(alert("j-" + jersey, "wait", "alertJersey", String.join(", ", names), "#" + jersey)));
        }
        if (attendance()) {
            for (Map<String, Object> row : attendanceService.atRisk()) {
                rows.add(alert("r-" + row.get("playerId"), "bad", "alertAttendance", String.valueOf(row.get("name")), row.get("attendancePercent") + "%"));
            }
        }
        if (injuries()) {
            for (Map<String, Object> row : openInjuries()) {
                rows.add(alert("i-" + row.get("playerId"), "wait", "alertInjured", String.valueOf(row.get("name")), String.valueOf(row.get("expectedReturn"))));
            }
        }
        if (executive()) {
            for (FinanceDtos.InstalmentView view : financeService.overdue()) {
                rows.add(alert("p-" + view.id(), "bad", "alertOverdue", view.name(), view.cashRemaining().toPlainString()));
            }
        }
        return rows;
    }

    public Object charts(String name, String period) {
        assertChart(name);
        List<FootballMatch> selected = finished(period);
        return switch (name) {
            case "goalsPerMatch" -> goalsPerMatch(selected);
            case "results" -> results(selected);
            case "squad" -> squad();
            case "minuteGoals" -> minuteGoals(selected);
            case "ages" -> ages();
            case "attendance" -> attendanceSeries();
            case "attendanceStatus" -> attendanceStatus();
            case "contribution" -> contribution(selected);
            case "minutes" -> minutes(selected, true);
            case "leastUsed" -> minutes(selected, false);
            case "injuryParts" -> injuryParts();
            case "injured" -> openInjuries();
            case "fines" -> finesSeries();
            case "payroll" -> payrollLines();
            case "salaryBands" -> salaryBands();
            case "payment" -> payment();
            case "cashout" -> cashout();
            case "earners" -> earners();
            case "bonuses" -> bonuses();
            case "partners" -> partners();
            default -> List.of();
        };
    }

    private void assertChart(String name) {
        if (FINANCE_CHARTS.contains(name) && !executive()) {
            throw new AccessDeniedException("Finance charts are limited to the executive office");
        }
        if (MEDICAL_CHARTS.contains(name) && !injuries()) {
            throw new AccessDeniedException("Injury charts are limited to the medical and coaching staff");
        }
        if (ATTENDANCE_CHARTS.contains(name) && !attendance()) {
            throw new AccessDeniedException("Attendance charts are limited to coaching and medical staff");
        }
        if (PARTNER_CHARTS.contains(name) && !partnersAllowed()) {
            throw new AccessDeniedException("Partner charts are limited to the commercial office");
        }
        if (!FINANCE_CHARTS.contains(name) && !MEDICAL_CHARTS.contains(name) && !ATTENDANCE_CHARTS.contains(name) && !PARTNER_CHARTS.contains(name) && !analysis()) {
            throw new AccessDeniedException("This chart is limited to the coaching staff");
        }
    }

    private List<Map<String, Object>> goalsPerMatch(List<FootballMatch> selected) {
        if (selected.isEmpty()) {
            return List.of();
        }
        List<FootballMatch> last = selected.size() > 10 ? selected.subList(selected.size() - 10, selected.size()) : selected;
        return last.stream().map(match -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", match.getOpponent().getName());
            row.put("scored", match.getGoalsFor());
            row.put("conceded", match.getGoalsAgainst());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> results(List<FootballMatch> selected) {
        if (selected.isEmpty()) {
            return List.of();
        }
        return List.of(
                Map.of("name", "wins", "value", wins(selected)),
                Map.of("name", "draws", "value", draws(selected)),
                Map.of("name", "losses", "value", losses(selected))
        );
    }

    private List<Map<String, Object>> squad() {
        List<Player> players = playerRepository.findByActiveTrueOrderByRegisterNoAsc();
        if (players.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Goalkeeper", 0);
        counts.put("Defender", 0);
        counts.put("Midfielder", 0);
        counts.put("Attacker", 0);
        for (Player player : players) {
            counts.merge(group(player.getPosition()), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "value", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> minuteGoals(List<FootballMatch> selected) {
        int[] bands = new int[6];
        int total = 0;
        for (FootballMatch match : selected) {
            for (MatchEvent event : eventRepository.findByMatchIdOrderByEventMinuteAsc(match.getId())) {
                if (event.getEventType() != EventType.GOAL && event.getEventType() != EventType.PENALTY_GOAL) {
                    continue;
                }
                int minute = event.getEventMinute();
                int index = Math.min(5, Math.max(0, (minute - 1) / 15));
                bands[index] += 1;
                total += 1;
            }
        }
        if (total == 0) {
            return List.of();
        }
        String[] labels = {"0-15", "16-30", "31-45", "46-60", "61-75", "76-90"};
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            rows.add(Map.of("name", labels[i], "value", bands[i]));
        }
        return rows;
    }

    private List<Map<String, Object>> ages() {
        int[] bands = new int[5];
        int total = 0;
        LocalDate today = LocalDate.now(clock);
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            if (player.getDateOfBirth() == null) {
                continue;
            }
            int age = Period.between(player.getDateOfBirth(), today).getYears();
            int index = age < 22 ? 0 : age < 26 ? 1 : age < 30 ? 2 : age < 34 ? 3 : 4;
            bands[index] += 1;
            total += 1;
        }
        if (total == 0) {
            return List.of();
        }
        String[] labels = {"18-21", "22-25", "26-29", "30-33", "34+"};
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (bands[i] > 0) {
                rows.add(Map.of("name", labels[i], "value", bands[i]));
            }
        }
        return rows;
    }

    private List<Map<String, Object>> attendanceSeries() {
        List<Map<String, Object>> weeks = attendanceService.weekly();
        if (weeks.isEmpty()) {
            return List.of();
        }
        return weeks.stream().map(week -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", "W" + week.get("week"));
            row.put("rate", week.get("attendancePercent"));
            row.put("target", 80);
            row.put("watch", 60);
            return row;
        }).toList();
    }

    private List<Map<String, Object>> attendanceStatus() {
        Map<String, Object> matrix = attendanceService.matrix();
        Object players = matrix.get("players");
        if (!(players instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        int onTrack = 0;
        int watch = 0;
        int atRisk = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> row) || row.get("attendancePercent") == null) {
                continue;
            }
            double percent = Double.parseDouble(row.get("attendancePercent").toString());
            if (percent >= 80) {
                onTrack += 1;
            } else if (percent >= 60) {
                watch += 1;
            } else {
                atRisk += 1;
            }
        }
        if (onTrack + watch + atRisk == 0) {
            return List.of();
        }
        return List.of(
                Map.of("name", "onTrack", "value", onTrack),
                Map.of("name", "watch", "value", watch),
                Map.of("name", "atRisk", "value", atRisk)
        );
    }

    private List<Map<String, Object>> contribution(List<FootballMatch> selected) {
        return rankRows(selected).stream()
                .filter(row -> ((Integer) row.get("goals") + (Integer) row.get("assists")) > 0)
                .limit(8)
                .map(row -> {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    copy.put("name", row.get("name"));
                    copy.put("goals", row.get("goals"));
                    copy.put("assists", row.get("assists"));
                    return copy;
                }).toList();
    }

    private List<Map<String, Object>> minutes(List<FootballMatch> selected, boolean most) {
        if (selected.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = rankRows(selected);
        if (most) {
            return rows.stream().filter(row -> (Integer) row.get("minutes") > 0).limit(10)
                    .map(row -> Map.<String, Object>of("name", row.get("name"), "minutes", row.get("minutes"), "jersey", row.get("jersey")))
                    .toList();
        }
        return rows.stream()
                .sorted(Comparator.comparingInt(row -> (Integer) row.get("minutes")))
                .limit(5)
                .map(row -> Map.<String, Object>of("id", row.get("id"), "name", row.get("name"), "jersey", row.get("jersey"), "minutes", row.get("minutes")))
                .toList();
    }

    private List<Map<String, Object>> injuryParts() {
        Map<String, Integer> parts = new LinkedHashMap<>();
        for (Map<String, Object> row : openInjuries()) {
            String part = String.valueOf(row.getOrDefault("bodyPart", "Other"));
            parts.merge(part, 1, Integer::sum);
        }
        return parts.entrySet().stream().map(entry -> Map.<String, Object>of("name", entry.getKey(), "value", entry.getValue())).toList();
    }

    private List<Map<String, Object>> finesSeries() {
        return fineService.byWeek().stream()
                .filter(row -> new BigDecimal(row.get("amount").toString()).signum() > 0)
                .map(row -> Map.<String, Object>of("name", "W" + row.get("week"), "amount", row.get("amount")))
                .toList();
    }

    private List<Map<String, Object>> payrollLines() {
        FinanceDtos.Summary money = safeFinance();
        if (money == null) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of("name", "Wages", "value", money.netBalanceDue()));
        rows.add(Map.of("name", "Advances", "value", money.advances()));
        rows.add(Map.of("name", "Envelopes", "value", money.envelopes()));
        rows.add(Map.of("name", "Social fund", "value", money.socialFund()));
        return rows.stream().filter(row -> new BigDecimal(row.get("value").toString()).signum() > 0).toList();
    }

    private List<Map<String, Object>> salaryBands() {
        FinanceDtos.Summary money = safeFinance();
        if (money == null) {
            return List.of();
        }
        return money.salaryBands().stream()
                .filter(band -> band.players() > 0)
                .map(band -> Map.<String, Object>of("name", band.band(), "value", band.players()))
                .toList();
    }

    private Map<String, Object> payment() {
        FinanceDtos.Summary money = safeFinance();
        if (money == null || money.seasonPayroll() == null || money.seasonPayroll().signum() == 0) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paid", money.paidToDate());
        body.put("balance", money.netBalanceDue());
        body.put("pct", money.paidPercent());
        return body;
    }

    private List<FinanceDtos.MonthRow> cashout() {
        FinanceDtos.Summary money = safeFinance();
        if (money == null) {
            return List.of();
        }
        return money.monthlySchedule().stream().filter(row -> row.wageBill().signum() > 0).toList();
    }

    private List<Map<String, Object>> earners() {
        FinanceDtos.Summary money = safeFinance();
        if (money == null) {
            return List.of();
        }
        return money.topEarners().stream()
                .filter(row -> row.seasonalPay().signum() > 0)
                .limit(10)
                .map(row -> Map.<String, Object>of("name", row.name(), "wage", row.seasonalPay()))
                .toList();
    }

    private List<Map<String, Object>> bonuses() {
        Map<String, BigDecimal> earned = new LinkedHashMap<>();
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        for (BonusEarned row : bonusRepository.findAll()) {
            if (row.getStatus() == BonusStatus.CANCELLED) {
                continue;
            }
            String name = row.getBonusType().name();
            earned.merge(name, row.getAmount(), BigDecimal::add);
            if (row.getStatus() == BonusStatus.PAID) {
                paid.merge(name, row.getAmount(), BigDecimal::add);
            }
        }
        return earned.entrySet().stream().map(entry -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", entry.getKey());
            row.put("earned", entry.getValue());
            row.put("paid", paid.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            return row;
        }).toList();
    }

    private List<Map<String, Object>> partners() {
        return partnerService.list().stream()
                .filter(row -> row.get("contractValue") != null && new BigDecimal(row.get("contractValue").toString()).signum() > 0)
                .map(row -> Map.<String, Object>of("name", row.get("name"), "income", row.get("contractValue")))
                .toList();
    }

    private List<Map<String, Object>> rank(String metric, String scope) {
        String period = "last5".equals(scope) || "5m".equals(scope) ? "5m" : "season";
        List<Map<String, Object>> rows = rankRows(finished(period)).stream()
                .filter(row -> (Integer) row.get(metric) > 0)
                .sorted(Comparator.comparingInt((Map<String, Object> row) -> (Integer) row.get(metric)).reversed())
                .toList();
        int leader = rows.isEmpty() ? 0 : (Integer) rows.get(0).get(metric);
        List<Map<String, Object>> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>(rows.get(i));
            row.put("rank", i + 1);
            row.put("leader", leader);
            ranked.add(row);
        }
        return ranked;
    }

    private List<Map<String, Object>> rankRows(List<FootballMatch> selected) {
        Map<Long, int[]> totals = new LinkedHashMap<>();
        for (FootballMatch match : selected) {
            for (MatchPlayerRecord record : recordRepository.findByMatchId(match.getId())) {
                int[] box = totals.computeIfAbsent(record.getPlayer().getId(), key -> new int[3]);
                box[0] += record.getGoals();
                box[1] += record.getAssists();
                box[2] += record.getMinutesPlayed();
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            int[] box = totals.getOrDefault(player.getId(), new int[3]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", player.getId());
            row.put("name", player.getKnownAs());
            row.put("jersey", player.getJerseyNumber());
            row.put("position", player.getPosition().getLabel());
            row.put("photoUrl", player.getPhotoUrl());
            row.put("goals", box[0]);
            row.put("assists", box[1]);
            row.put("minutes", box[2]);
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> playedCard(FootballMatch match) {
        Map<String, Object> body = fixture(match);
        String result = match.getGoalsFor() > match.getGoalsAgainst() ? "W" : match.getGoalsFor() < match.getGoalsAgainst() ? "L" : "D";
        body.put("result", result);
        body.put("scoreFor", match.getGoalsFor());
        body.put("scoreAgainst", match.getGoalsAgainst());
        List<String> scorers = new ArrayList<>();
        List<Map<String, Object>> events = new ArrayList<>();
        for (MatchEvent event : eventRepository.findByMatchIdOrderByEventMinuteAsc(match.getId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("minute", event.getEventMinute());
            row.put("type", event.getEventType().name());
            row.put("player", event.getPlayer() == null ? "" : event.getPlayer().getKnownAs());
            row.put("related", event.getRelatedPlayer() == null ? "" : event.getRelatedPlayer().getKnownAs());
            events.add(row);
            if (event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL) {
                String name = event.getPlayer() == null ? "" : event.getPlayer().getKnownAs();
                scorers.add(name + " " + event.getEventMinute() + "'");
            }
        }
        body.put("scorers", scorers);
        body.put("events", events);
        return body;
    }

    private Map<String, Object> fixture(FootballMatch match) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", match.getId());
        body.put("opponent", match.getOpponent().getName());
        body.put("logoUrl", match.getOpponent().getLogoUrl());
        body.put("kickoff", match.getKickoff());
        body.put("date", match.getKickoff().toString().substring(0, 10));
        body.put("time", match.getKickoff().toString().substring(11, 16));
        body.put("venue", match.getVenue());
        body.put("home", match.isHomeMatch());
        body.put("competition", match.getCompetition());
        body.put("lineup", match.getStatus() == MatchStatus.LINEUP_PUBLISHED || match.getStatus() == MatchStatus.LIVE || match.getStatus() == MatchStatus.FINISHED
                ? "published" : "unpublished");
        body.put("status", match.getStatus().name());
        return body;
    }

    private Map<String, Object> goalRow(FootballMatch match, MatchEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("minute", event.getEventMinute());
        row.put("scorer", event.getPlayer() == null ? "" : event.getPlayer().getKnownAs());
        row.put("photoUrl", event.getPlayer() == null ? null : event.getPlayer().getPhotoUrl());
        row.put("assist", event.getRelatedPlayer() == null ? null : event.getRelatedPlayer().getKnownAs());
        row.put("opponent", match.getOpponent().getName());
        row.put("date", match.getKickoff().toString().substring(0, 10));
        return row;
    }

    private Map<String, Object> trends(List<FootballMatch> current, List<FootballMatch> previous) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (previous.isEmpty()) {
            return body;
        }
        putTrend(body, "played", current.size(), previous.size());
        putTrend(body, "goalsFor", sum(current, FootballMatch::getGoalsFor), sum(previous, FootballMatch::getGoalsFor));
        putTrend(body, "goalsAgainst", sum(current, FootballMatch::getGoalsAgainst), sum(previous, FootballMatch::getGoalsAgainst));
        putTrend(body, "winRate", rate(wins(current), current.size()), rate(wins(previous), previous.size()));
        return body;
    }

    private void putTrend(Map<String, Object> body, String key, double current, double previous) {
        if (previous == 0) {
            return;
        }
        body.put(key, Math.round(((current - previous) / previous) * 1000) / 10.0);
    }

    private Map<String, Object> sparks(List<FootballMatch> current) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (current.size() < 2) {
            return body;
        }
        List<Integer> played = new ArrayList<>();
        List<Integer> goalsFor = new ArrayList<>();
        List<Integer> goalsAgainst = new ArrayList<>();
        int gf = 0;
        int ga = 0;
        for (int i = 0; i < current.size(); i++) {
            played.add(i + 1);
            gf += current.get(i).getGoalsFor();
            ga += current.get(i).getGoalsAgainst();
            goalsFor.add(gf);
            goalsAgainst.add(ga);
        }
        body.put("played", played);
        body.put("goalsFor", goalsFor);
        body.put("goalsAgainst", goalsAgainst);
        return body;
    }

    private Double attendanceRate() {
        List<Map<String, Object>> weeks = attendanceService.weekly();
        if (weeks.isEmpty()) {
            return null;
        }
        double total = 0;
        int count = 0;
        for (Map<String, Object> week : weeks) {
            if (week.get("attendancePercent") == null) {
                continue;
            }
            total += Double.parseDouble(week.get("attendancePercent").toString());
            count += 1;
        }
        return count == 0 ? null : Math.round(total / count * 10) / 10.0;
    }

    private List<FootballMatch> finished(String period) {
        List<FootballMatch> finished = seasonMatches().stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .sorted(Comparator.comparing(FootballMatch::getKickoff))
                .toList();
        if ("30d".equals(period)) {
            Instant from = Instant.now(clock).minus(30, ChronoUnit.DAYS);
            return finished.stream().filter(match -> !match.getKickoff().isBefore(from)).toList();
        }
        if ("5m".equals(period) || "last5".equals(period)) {
            return finished.size() <= 5 ? finished : finished.subList(finished.size() - 5, finished.size());
        }
        return finished;
    }

    private List<FootballMatch> previousFinished(String period) {
        List<FootballMatch> finished = seasonMatches().stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .sorted(Comparator.comparing(FootballMatch::getKickoff))
                .toList();
        if ("30d".equals(period)) {
            Instant end = Instant.now(clock).minus(30, ChronoUnit.DAYS);
            Instant start = end.minus(30, ChronoUnit.DAYS);
            return finished.stream().filter(match -> !match.getKickoff().isBefore(start) && match.getKickoff().isBefore(end)).toList();
        }
        if (("5m".equals(period) || "last5".equals(period)) && finished.size() > 5) {
            int end = finished.size() - 5;
            int start = Math.max(0, end - 5);
            return finished.subList(start, end);
        }
        return List.of();
    }

    private List<FootballMatch> seasonMatches() {
        Long seasonId = seasonId();
        if (seasonId == null) {
            return List.of();
        }
        return matchRepository.findBySeasonIdOrderByKickoffAsc(seasonId);
    }

    private Long seasonId() {
        try {
            return settingsService.currentSeason().getId();
        } catch (NotFoundException ex) {
            return null;
        }
    }

    private FinanceDtos.Summary safeFinance() {
        try {
            return financeService.summary();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> openInjuries() {
        Object rows = medicalService.dashboard().get("notCleared");
        if (rows instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private String group(Position position) {
        return switch (position) {
            case GOALKEEPER -> "Goalkeeper";
            case CENTER_BACK, LEFT_BACK, RIGHT_BACK -> "Defender";
            case DEFENSIVE_MIDFIELDER, CENTRAL_MIDFIELDER, OFFENSIVE_MIDFIELDER -> "Midfielder";
            case WINGER, FORWARD -> "Attacker";
        };
    }

    private int wins(List<FootballMatch> matches) {
        return (int) matches.stream().filter(match -> match.getGoalsFor() > match.getGoalsAgainst()).count();
    }

    private int draws(List<FootballMatch> matches) {
        return (int) matches.stream().filter(match -> match.getGoalsFor() == match.getGoalsAgainst()).count();
    }

    private int losses(List<FootballMatch> matches) {
        return (int) matches.stream().filter(match -> match.getGoalsFor() < match.getGoalsAgainst()).count();
    }

    private int sum(List<FootballMatch> matches, java.util.function.ToIntFunction<FootballMatch> reader) {
        return matches.stream().mapToInt(reader).sum();
    }

    private double rate(int wins, int played) {
        return played == 0 ? 0 : Math.round((wins * 1000.0) / played) / 10.0;
    }

    private <T> PageResponse<T> page(List<T> rows, int page, int size) {
        int safeSize = clamp(size);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        int pages = rows.isEmpty() ? 0 : (int) Math.ceil(rows.size() / (double) safeSize);
        return new PageResponse<>(rows.subList(from, to), safePage, safeSize, rows.size(), pages);
    }

    private int clamp(int size) {
        if (size < 1) {
            return 5;
        }
        return Math.min(size, 50);
    }

    private Map<String, Object> alert(String id, String tone, String key, String name, String detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("tone", tone);
        row.put("key", key);
        row.put("name", name);
        row.put("detail", detail == null || "null".equals(detail) ? "" : detail);
        return row;
    }

    private boolean executive() {
        return SecurityUtils.isExecutive();
    }

    private boolean coach() {
        return SecurityUtils.hasRole("HEAD_COACH") || SecurityUtils.hasRole("ASSISTANT_COACH")
                || SecurityUtils.hasRole("GOALKEEPER_COACH") || SecurityUtils.hasRole("ASSISTANT_MANAGER");
    }

    private boolean media() {
        return SecurityUtils.hasRole("SOCIAL_MEDIA_MANAGER") || SecurityUtils.hasRole("MARKETING_MANAGER");
    }

    private boolean doctor() {
        return SecurityUtils.hasRole("DOCTOR");
    }

    private boolean sport() {
        return executive() || coach() || media();
    }

    private boolean people() {
        return executive() || coach() || doctor();
    }

    private boolean attendance() {
        return executive() || coach() || doctor();
    }

    private boolean injuries() {
        return executive() || coach() || doctor();
    }

    private boolean analysis() {
        return executive() || coach();
    }

    private boolean partnersAllowed() {
        return executive() || media();
    }
}
