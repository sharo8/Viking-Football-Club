package rw.vikingfc.club.competition;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.match.MatchService;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.match.Opponent;
import rw.vikingfc.club.match.OpponentRepository;
import rw.vikingfc.club.match.StandingsService;
import rw.vikingfc.club.medical.MedicalCheck;
import rw.vikingfc.club.medical.MedicalCheckRepository;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.training.TrainingSession;
import rw.vikingfc.club.training.TrainingSessionRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    private final FootballMatchRepository matchRepository;
    private final LeagueFixtureRepository fixtureRepository;
    private final TrainingSessionRepository trainingRepository;
    private final PlayerRepository playerRepository;
    private final MedicalCheckRepository medicalCheckRepository;
    private final OpponentRepository opponentRepository;
    private final CompetitionRepository competitionRepository;
    private final SettingsService settingsService;
    private final MatchService matchService;
    private final StandingsService standingsService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> events(Instant from, Instant to, String layers) {
        String wanted = layers == null ? "matches,deadlines" : layers;
        List<Map<String, Object>> rows = new ArrayList<>();
        Long seasonId = settingsService.currentSeason().getId();
        if (wanted.contains("matches") || wanted.contains("deadlines")) {
            for (FootballMatch match : matchRepository.findBySeasonIdOrderByKickoffAsc(seasonId)) {
                if (match.getKickoff().isBefore(from) || !match.getKickoff().isBefore(to)) {
                    continue;
                }
                if (wanted.contains("matches")) {
                    rows.add(event("MATCH", match.getId(), match.getKickoff(), match.getOpponent().getName(), match.getCompetition(), match.isHomeMatch()));
                }
                if (wanted.contains("deadlines")) {
                    rows.add(event("DEADLINE", match.getId(), match.getKickoff().minusSeconds(match.getAlertHours() * 3600L),
                            "Lineup deadline vs " + match.getOpponent().getName(), match.getCompetition(), match.isHomeMatch()));
                }
            }
            for (LeagueFixture fixture : fixtureRepository.findByKickoffBetweenOrderByKickoffAsc(from, to)) {
                rows.add(event("FIXTURE", fixture.getId(), fixture.getKickoff(),
                        fixture.getHome().getName() + " vs " + fixture.getAway().getName(),
                        fixture.getCompetition().getName(), false));
            }
        }
        LocalDate start = LocalDate.ofInstant(from, KIGALI);
        LocalDate end = LocalDate.ofInstant(to, KIGALI);
        if (wanted.contains("training")) {
            for (TrainingSession session : trainingRepository.findBySeasonIdOrderBySessionDateAsc(seasonId)) {
                if (!session.getSessionDate().isBefore(start) && session.getSessionDate().isBefore(end.plusDays(1))) {
                    rows.add(event("TRAINING", session.getId(), session.getSessionDate().atStartOfDay(KIGALI).toInstant(), "Training", null, true));
                }
            }
        }
        if (wanted.contains("birthdays")) {
            for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
                if (player.getDateOfBirth() == null) {
                    continue;
                }
                LocalDate day = player.getDateOfBirth().withYear(start.getYear());
                if (day.isBefore(start)) {
                    day = day.plusYears(1);
                }
                if (!day.isAfter(end)) {
                    rows.add(event("BIRTHDAY", player.getId(), day.atStartOfDay(KIGALI).toInstant(), player.getKnownAs(), null, true));
                }
            }
        }
        if (wanted.contains("medical")) {
            for (MedicalCheck check : medicalCheckRepository.findByCheckDateBetween(start, end)) {
                String name = check.getInjury().getPlayer().getKnownAs();
                rows.add(event("MEDICAL", check.getId(), check.getCheckDate().atStartOfDay(KIGALI).toInstant(), name, null, true));
            }
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> fixture(Long competitionId, Long homeId, Long awayId, Instant kickoff, Integer matchday, Integer goalsHome, Integer goalsAway, MatchStatus status) {
        LeagueFixture fixture = new LeagueFixture();
        fixture.setCompetition(competitionRepository.findById(competitionId).orElseThrow(() -> NotFoundException.of("Competition", competitionId)));
        fixture.setHome(opponent(homeId));
        fixture.setAway(opponent(awayId));
        fixture.setKickoff(kickoff);
        fixture.setMatchday(matchday);
        fixture.setStatus(status == null ? MatchStatus.SCHEDULED : status);
        fixture.setGoalsHome(goalsHome == null ? 0 : goalsHome);
        fixture.setGoalsAway(goalsAway == null ? 0 : goalsAway);
        fixtureRepository.save(fixture);
        if (fixture.getStatus() == MatchStatus.FINISHED && fixture.getCompetition().isCountsForTable()) {
            standingsService.recalculate(fixture.getCompetition().getId());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", fixture.getId());
        body.put("home", fixture.getHome().getName());
        body.put("away", fixture.getAway().getName());
        body.put("status", fixture.getStatus());
        return body;
    }

    public String ics(Instant from, Instant to) {
        StringBuilder text = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Viking FC//Calendar//EN\r\n");
        for (Map<String, Object> row : events(from, to, "matches,training")) {
            text.append("BEGIN:VEVENT\r\nSUMMARY:").append(row.get("title")).append("\r\nDTSTART:")
                    .append(String.valueOf(row.get("at")).replace("-", "").replace(":", "").replace(".", ""))
                    .append("\r\nEND:VEVENT\r\n");
        }
        text.append("END:VCALENDAR\r\n");
        return text.toString();
    }

    public Map<String, Object> reschedule(Long matchId, Instant kickoff) {
        return matchService.reschedule(matchId, kickoff);
    }

    private Opponent opponent(Long id) {
        return opponentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Opponent", id));
    }

    private Map<String, Object> event(String kind, Long id, Instant at, String title, String competition, boolean home) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", kind);
        body.put("id", id);
        body.put("at", at);
        body.put("title", title);
        body.put("competition", competition);
        body.put("home", home);
        return body;
    }
}
