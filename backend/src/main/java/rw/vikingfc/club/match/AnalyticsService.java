package rw.vikingfc.club.match;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceCalculator;
import rw.vikingfc.club.attendance.AttendanceMark;
import rw.vikingfc.club.attendance.AttendanceRecord;
import rw.vikingfc.club.attendance.AttendanceRecordRepository;
import rw.vikingfc.club.bonus.BonusEarned;
import rw.vikingfc.club.bonus.BonusEarnedRepository;
import rw.vikingfc.club.bonus.BonusStatus;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.sanction.FineStatus;
import rw.vikingfc.club.settings.SettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final PlayerRepository playerRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final FootballMatchRepository matchRepository;
    private final PlayerContractRepository contractRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final FineRepository fineRepository;
    private final BonusEarnedRepository bonusRepository;
    private final SettingsService settingsService;

    public Map<String, Object> playerStats(Long playerId) {
        Player player = playerRepository.findByIdAndActiveTrue(playerId)
                .orElseThrow(() -> NotFoundException.of("Player", playerId));
        List<MatchPlayerRecord> rows = recordRepository.findByPlayerId(playerId);
        int played = (int) rows.stream().filter(MatchPlayerRecord::isParticipated).count();
        int starts = (int) rows.stream().filter(row -> row.getParticipation() == Participation.STARTED).count();
        int subUsed = (int) rows.stream().filter(row -> row.getParticipation() == Participation.SUB_USED).count();
        int unused = (int) rows.stream().filter(row -> row.getParticipation() == Participation.SUB_UNUSED).count();
        int notSelected = (int) rows.stream().filter(row -> row.getParticipation() == Participation.NOT_SELECTED).count();
        int minutes = rows.stream().mapToInt(MatchPlayerRecord::getMinutesPlayed).sum();
        int goals = rows.stream().mapToInt(MatchPlayerRecord::getGoals).sum();
        int assists = rows.stream().mapToInt(MatchPlayerRecord::getAssists).sum();
        int yellow = rows.stream().mapToInt(MatchPlayerRecord::getYellowCards).sum();
        int red = rows.stream().mapToInt(MatchPlayerRecord::getRedCards).sum();
        List<BigDecimal> ratings = rows.stream().map(MatchPlayerRecord::getRating).filter(rating -> rating != null).toList();
        BigDecimal averageRating = ratings.isEmpty() ? null
                : ratings.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
        List<AttendanceRecord> attendance = attendanceRepository.findByPlayerId(playerId);
        int present = count(attendance, AttendanceMark.P);
        int late = count(attendance, AttendanceMark.L);
        int excused = count(attendance, AttendanceMark.E);
        int absent = count(attendance, AttendanceMark.A);
        BigDecimal bonus = bonusRepository.findByPlayerIdOrderByEarnedOnDesc(playerId).stream()
                .filter(row -> row.getStatus() != BonusStatus.CANCELLED)
                .map(BonusEarned::getAmount).reduce(Money.zero(), BigDecimal::add);
        BigDecimal fines = fineRepository.findByPlayerIdOrderByFineDateAsc(playerId).stream()
                .filter(fine -> fine.getStatus() != FineStatus.WAIVED)
                .map(Fine::getAmount).reduce(Money.zero(), BigDecimal::add);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerId", player.getId());
        body.put("name", player.getFullName());
        body.put("knownAs", player.getKnownAs());
        body.put("matchesPlayed", played);
        body.put("starts", starts);
        body.put("substituteAppearances", subUsed);
        body.put("unusedSubstitute", unused);
        body.put("notSelected", notSelected);
        body.put("minutes", minutes);
        body.put("goals", goals);
        body.put("assists", assists);
        body.put("goalContributions", goals + assists);
        body.put("yellowCards", yellow);
        body.put("redCards", red);
        body.put("averageRating", averageRating);
        body.put("attendancePercent", AttendanceCalculator.percentage(present, late, excused, absent));
        if (SecurityUtils.isExecutive()) {
            body.put("bonusEarned", bonus);
        }
        body.put("fines", fines);
        return body;
    }

    public Map<String, Object> leaderboards() {
        Long seasonId = settingsService.currentSeason().getId();
        List<Player> players = playerRepository.findByActiveTrueOrderByRegisterNoAsc();
        List<FootballMatch> finished = matchRepository.findBySeasonIdAndStatusOrderByKickoffDesc(seasonId, MatchStatus.FINISHED);
        int availableMinutes = finished.size() * settingsService.current().getMatchLengthMinutes();
        Map<Long, PlayerContract> contracts = new LinkedHashMap<>();
        for (PlayerContract contract : contractRepository.findBySeasonIdAndActiveTrue(seasonId)) {
            contracts.put(contract.getPlayer().getId(), contract);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Player player : players) {
            List<MatchPlayerRecord> records = recordRepository.findByPlayerId(player.getId());
            int goals = records.stream().mapToInt(MatchPlayerRecord::getGoals).sum();
            int assists = records.stream().mapToInt(MatchPlayerRecord::getAssists).sum();
            int minutes = records.stream().mapToInt(MatchPlayerRecord::getMinutesPlayed).sum();
            int played = (int) records.stream().filter(MatchPlayerRecord::isParticipated).count();
            PlayerContract contract = contracts.get(player.getId());
            BigDecimal pay = contract == null ? Money.zero() : contract.getSeasonalPay();
            BigDecimal fines = fineRepository.findByPlayerIdOrderByFineDateAsc(player.getId()).stream()
                    .filter(fine -> fine.getStatus() != FineStatus.WAIVED)
                    .map(Fine::getAmount).reduce(Money.zero(), BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", player.getId());
            row.put("name", player.getKnownAs());
            row.put("position", player.getPosition().getLabel());
            row.put("line", player.getLineName());
            row.put("goals", goals);
            row.put("assists", assists);
            row.put("contributions", goals + assists);
            row.put("minutes", minutes);
            row.put("played", played);
            row.put("seasonalPay", pay);
            row.put("fines", fines);
            row.put("costPerGoal", goals == 0 ? null : pay.divide(BigDecimal.valueOf(goals), 0, RoundingMode.HALF_UP));
            row.put("costPerMatch", played == 0 ? null : pay.divide(BigDecimal.valueOf(played), 0, RoundingMode.HALF_UP));
            row.put("shareOfAvailableMinutes", availableMinutes == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(minutes).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(availableMinutes), 1, RoundingMode.HALF_UP));
            rows.add(row);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topScorers", top(rows, "goals"));
        body.put("topAssists", top(rows, "assists"));
        body.put("topContributions", top(rows, "contributions"));
        body.put("mostMinutes", top(rows, "minutes"));
        body.put("mostExpensive", top(rows, "seasonalPay"));
        body.put("mostFined", top(rows, "fines"));
        body.put("neverPlayed", rows.stream().filter(row -> ((Integer) row.get("played")) == 0).toList());
        body.put("leastUsed", rows.stream()
                .filter(row -> ((BigDecimal) row.get("shareOfAvailableMinutes")).compareTo(new BigDecimal("20.0")) < 0)
                .sorted(Comparator.comparing(row -> (BigDecimal) row.get("shareOfAvailableMinutes")))
                .toList());
        body.put("bestValue", rows.stream().filter(row -> row.get("costPerGoal") != null)
                .sorted(Comparator.comparing(row -> (BigDecimal) row.get("costPerGoal")))
                .limit(10).toList());
        if (!SecurityUtils.isExecutive()) {
            stripMoney(body);
        }
        return body;
    }

    private List<Map<String, Object>> top(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .sorted(Comparator.comparing((Map<String, Object> row) -> new BigDecimal(row.get(key).toString())).reversed())
                .limit(10)
                .toList();
    }

    private void stripMoney(Object node) {
        if (node instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) raw;
            map.remove("seasonalPay");
            map.remove("costPerGoal");
            map.remove("costPerMatch");
            map.remove("bonusEarned");
            map.values().forEach(this::stripMoney);
        } else if (node instanceof Iterable<?> list) {
            list.forEach(this::stripMoney);
        }
    }

    private int count(List<AttendanceRecord> rows, AttendanceMark mark) {
        return (int) rows.stream().filter(row -> row.getStatus() == mark).count();
    }
}
