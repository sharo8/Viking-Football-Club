package rw.vikingfc.club.match;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.competition.Competition;
import rw.vikingfc.club.competition.CompetitionRepository;
import rw.vikingfc.club.competition.LeagueFixture;
import rw.vikingfc.club.competition.LeagueFixtureRepository;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.Role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandingsService {

    private final CompetitionRepository competitionRepository;
    private final FootballMatchRepository matchRepository;
    private final LeagueFixtureRepository fixtureRepository;
    private final StandingRepository standingRepository;
    private final StandingAdjustmentRepository adjustmentRepository;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messaging;
    private final AuditService auditService;

    @Transactional
    public List<Map<String, Object>> recalculate(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> NotFoundException.of("Competition", competitionId));
        if (!competition.isCountsForTable()) {
            return List.of();
        }
        String club = settingsService.current().getClubName() == null ? "Viking FC" : settingsService.current().getClubName();
        Map<String, StandingsMath.Row> table = StandingsMath.table();
        List<FootballMatch> matches = matchRepository.findBySeasonIdOrderByKickoffAsc(competition.getSeason().getId()).stream()
                .filter(match -> match.getCompetitionRef() != null && competitionId.equals(match.getCompetitionRef().getId()))
                .filter(match -> match.isOfficial() && match.getStatus() == MatchStatus.FINISHED)
                .sorted(Comparator.comparing(FootballMatch::getKickoff))
                .toList();
        for (FootballMatch match : matches) {
            StandingsMath.apply(
                    StandingsMath.row(table, club, true),
                    StandingsMath.row(table, match.getOpponent().getName(), false),
                    match.getGoalsFor(),
                    match.getGoalsAgainst());
        }
        for (LeagueFixture fixture : fixtureRepository.findByCompetitionIdOrderByKickoffAsc(competitionId)) {
            if (fixture.getStatus() != MatchStatus.FINISHED) {
                continue;
            }
            StandingsMath.apply(
                    StandingsMath.row(table, fixture.getHome().getName(), false),
                    StandingsMath.row(table, fixture.getAway().getName(), false),
                    fixture.getGoalsHome(),
                    fixture.getGoalsAway());
        }
        Map<String, Integer> deltas = adjustmentRepository.findByCompetitionId(competitionId).stream()
                .collect(Collectors.toMap(row -> row.getTeamName().toLowerCase(), StandingAdjustment::getPointsDelta, Integer::sum));
        List<StandingsMath.Row> ranked = new ArrayList<>(StandingsMath.rank(table.values(), competition.getWinPoints(), competition.getDrawPoints(), competition.getLossPoints()));
        ranked.sort(Comparator.comparingInt((StandingsMath.Row row) -> row.points(competition.getWinPoints(), competition.getDrawPoints(), competition.getLossPoints())
                        + deltas.getOrDefault(row.name.toLowerCase(), 0)).reversed()
                .thenComparing(Comparator.comparingInt(StandingsMath.Row::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt((StandingsMath.Row row) -> row.goalsFor).reversed())
                .thenComparing(row -> row.name, String.CASE_INSENSITIVE_ORDER));
        Set<String> names = ranked.stream().map(row -> row.name).collect(Collectors.toSet());
        int place = 1;
        List<Map<String, Object>> body = new ArrayList<>();
        for (StandingsMath.Row row : ranked) {
            Standing standing = standingRepository.findBySeasonIdAndTeamName(competition.getSeason().getId(), row.name).orElseGet(Standing::new);
            standing.setSeason(competition.getSeason());
            standing.setCompetition(competition);
            standing.setTeamName(row.name);
            standing.setPositionNo(place++);
            standing.setPlayed(row.played);
            standing.setWon(row.won);
            standing.setDrawn(row.drawn);
            standing.setLost(row.lost);
            standing.setGoalsFor(row.goalsFor);
            standing.setGoalsAgainst(row.goalsAgainst);
            standing.setPoints(row.points(competition.getWinPoints(), competition.getDrawPoints(), competition.getLossPoints())
                    + deltas.getOrDefault(row.name.toLowerCase(), 0));
            standing.setFormText(row.formText());
            standing.setClubRow(row.club);
            standing.setSourceKind("COMPUTED");
            standingRepository.save(standing);
            body.add(view(standing));
        }
        for (Standing existing : standingRepository.findBySeasonIdOrderByPositionNoAsc(competition.getSeason().getId())) {
            if ("COMPUTED".equals(existing.getSourceKind())
                    && existing.getCompetition() != null
                    && competitionId.equals(existing.getCompetition().getId())
                    && !names.contains(existing.getTeamName())) {
                standingRepository.delete(existing);
            }
        }
        messaging.convertAndSend("/topic/standings", Map.of("type", "STANDINGS_UPDATED", "competitionId", competitionId));
        notificationService.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER, Role.HEAD_COACH, Role.ASSISTANT_MANAGER),
                "STANDINGS", "League table updated", competition.getName(), "/admin/table");
        auditService.record("RECALCULATE", "Standings", competitionId, null, names.toString(), null);
        return body;
    }

    @Transactional
    public Map<String, Object> correct(Long competitionId, String teamName, int pointsDelta, String reason) {
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("A reason is required for a table correction", "RESULT_MISMATCH");
        }
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> NotFoundException.of("Competition", competitionId));
        StandingAdjustment adjustment = adjustmentRepository.findByCompetitionIdAndTeamNameIgnoreCase(competitionId, teamName)
                .orElseGet(StandingAdjustment::new);
        adjustment.setCompetition(competition);
        adjustment.setTeamName(teamName);
        adjustment.setPointsDelta(pointsDelta);
        adjustment.setReason(reason);
        adjustmentRepository.save(adjustment);
        auditService.record("CORRECT", "Standings", competitionId, null, teamName + " " + pointsDelta + " " + reason, null);
        recalculate(competitionId);
        return Map.of("team", teamName, "pointsDelta", pointsDelta);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> table(Long competitionId) {
        return standingRepository.findBySeasonIdOrderByPositionNoAsc(settingsService.currentSeason().getId()).stream()
                .filter(row -> competitionId == null || (row.getCompetition() != null && competitionId.equals(row.getCompetition().getId())))
                .map(this::view)
                .toList();
    }

    private Map<String, Object> view(Standing row) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", row.getId());
        body.put("team", row.getTeamName());
        body.put("position", row.getPositionNo());
        body.put("played", row.getPlayed());
        body.put("won", row.getWon());
        body.put("drawn", row.getDrawn());
        body.put("lost", row.getLost());
        body.put("goalsFor", row.getGoalsFor());
        body.put("goalsAgainst", row.getGoalsAgainst());
        body.put("goalDifference", row.getGoalsFor() - row.getGoalsAgainst());
        body.put("points", row.getPoints());
        body.put("form", row.getFormText());
        body.put("club", row.isClubRow());
        body.put("competitionId", row.getCompetition() == null ? null : row.getCompetition().getId());
        return body;
    }
}
