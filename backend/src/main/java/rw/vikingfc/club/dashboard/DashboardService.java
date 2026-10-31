package rw.vikingfc.club.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceService;
import rw.vikingfc.club.bonus.BonusService;
import rw.vikingfc.club.finance.FinanceDtos;
import rw.vikingfc.club.finance.FinanceService;
import rw.vikingfc.club.match.AnalyticsService;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.medical.MedicalService;
import rw.vikingfc.club.partner.PartnerService;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.sanction.FineService;
import rw.vikingfc.club.settings.SettingsService;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final FinanceService financeService;
    private final AttendanceService attendanceService;
    private final FineService fineService;
    private final BonusService bonusService;
    private final MedicalService medicalService;
    private final PartnerService partnerService;
    private final AnalyticsService analyticsService;
    private final FootballMatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;
    private final Clock clock;

    public Map<String, Object> executive() {
        FinanceDtos.Summary summary = financeService.summary();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", summary);
        body.put("attendance", attendanceService.kpis(null, null, null, null, null));
        body.put("fines", fineService.summary());
        body.put("bonuses", bonusService.liability());
        body.put("partners", partnerService.report());
        body.put("medical", medicalService.dashboard());
        body.put("upcomingMatches", upcoming());
        body.put("squadSize", playerRepository.countByActiveTrue());
        return body;
    }

    public Map<String, Object> coach() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("upcomingMatches", upcoming());
        body.put("availability", medicalService.availability(null));
        body.put("atRisk", attendanceService.atRisk());
        body.put("attendance", attendanceService.kpis(null, null, null, null, null));
        body.put("fines", fineService.summary());
        Map<String, Object> boards = analyticsService.leaderboards();
        stripMoney(boards);
        body.put("leaderboards", boards);
        return body;
    }

    public Map<String, Object> assistant() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("attendance", attendanceService.kpis(null, null, null, null, null));
        body.put("weekly", attendanceService.weekly());
        body.put("fines", fineService.summary());
        body.put("upcomingMatches", upcoming());
        body.put("availability", medicalService.availability(null));
        return body;
    }

    public Map<String, Object> media() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("upcomingMatches", upcoming());
        body.put("newSignings", playerRepository.findByActiveTrueAndNewSigningTrueOrderBySignedOnDesc().stream()
                .map(player -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", player.getId());
                    row.put("name", player.getFullName());
                    row.put("knownAs", player.getKnownAs());
                    row.put("jersey", player.getJerseyNumber());
                    row.put("position", player.getPosition().getLabel());
                    row.put("photoUrl", player.getPhotoUrl());
                    row.put("signedOn", player.getSignedOn());
                    return row;
                }).toList());
        return body;
    }

    public Map<String, Object> charts(String name) {
        FinanceDtos.Summary summary = financeService.summary();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chart", name);
        body.put("payrollByLine", summary.payrollByLine());
        body.put("salaryBands", summary.salaryBands());
        body.put("paymentStatus", summary.paymentStatus());
        body.put("monthlySchedule", summary.monthlySchedule());
        body.put("attendanceByWeek", attendanceService.weekly());
        body.put("finesByWeek", fineService.byWeek());
        return body;
    }

    private List<Map<String, Object>> upcoming() {
        Instant now = Instant.now(clock);
        return matchRepository.findBySeasonIdOrderByKickoffAsc(settingsService.currentSeason().getId()).stream()
                .filter(match -> !match.getKickoff().isBefore(now) && match.getStatus() != MatchStatus.CANCELLED)
                .limit(5)
                .map(this::fixture)
                .toList();
    }

    private void stripMoney(Object node) {
        if (node instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) raw;
            map.remove("seasonalPay");
            map.remove("costPerGoal");
            map.remove("costPerMatch");
            map.values().forEach(this::stripMoney);
        } else if (node instanceof List<?> list) {
            list.forEach(this::stripMoney);
        }
    }

    private Map<String, Object> fixture(FootballMatch match) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", match.getId());
        body.put("opponent", match.getOpponent().getName());
        body.put("kickoff", match.getKickoff());
        body.put("venue", match.getVenue());
        body.put("home", match.isHomeMatch());
        body.put("status", match.getStatus());
        return body;
    }
}
