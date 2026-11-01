package rw.vikingfc.club.match;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.bonus.BonusService;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.competition.Competition;
import rw.vikingfc.club.competition.CompetitionRepository;
import rw.vikingfc.club.competition.CompetitionType;
import rw.vikingfc.club.config.JpaConfig;
import rw.vikingfc.club.lineup.Convocation;
import rw.vikingfc.club.lineup.ConvocationRepository;
import rw.vikingfc.club.lineup.LineupSlot;
import rw.vikingfc.club.lineup.LineupSlotRepository;
import rw.vikingfc.club.lineup.LineupState;
import rw.vikingfc.club.lineup.LineupVersion;
import rw.vikingfc.club.lineup.LineupVersionRepository;
import rw.vikingfc.club.lineup.Selection;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.sanction.FineService;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.Role;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final FootballMatchRepository matchRepository;
    private final OpponentRepository opponentRepository;
    private final MatchEventRepository eventRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final LineupVersionRepository lineupRepository;
    private final LineupSlotRepository slotRepository;
    private final StandingRepository standingRepository;
    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;
    private final BonusService bonusService;
    private final FineService fineService;
    private final EntityManager entityManager;
    private final CompetitionRepository competitionRepository;
    private final ConvocationRepository convocationRepository;
    private final StandingsService standingsService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return search(null, null, null, null, null, null);
    }

    public List<Map<String, Object>> search(Long competitionId, String type, String status, Boolean home, String query, Boolean needsAttention) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        return matchRepository.findBySeasonIdOrderByKickoffAsc(settingsService.currentSeason().getId()).stream()
                .filter(match -> competitionId == null || (match.getCompetitionRef() != null && competitionId.equals(match.getCompetitionRef().getId())))
                .filter(match -> type == null || type.isBlank() || (match.getCompetitionRef() != null && match.getCompetitionRef().getType().name().equalsIgnoreCase(type)))
                .filter(match -> status == null || status.isBlank() || match.getStatus().name().equalsIgnoreCase(status))
                .filter(match -> home == null || match.isHomeMatch() == home)
                .filter(match -> needle.isEmpty() || match.getOpponent().getName().toLowerCase().contains(needle) || match.getCompetition().toLowerCase().contains(needle))
                .map(this::view)
                .filter(row -> needsAttention == null || !needsAttention || !((List<?>) row.get("alerts")).isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        return view(load(id));
    }

    public Map<String, Object> create(MatchWrite request) {
        FootballMatch match = new FootballMatch();
        match.setSeason(settingsService.currentSeason());
        match.setGoalsFor(0);
        match.setGoalsAgainst(0);
        match.setStoppageMinutes(0);
        match.setStatus(MatchStatus.SCHEDULED);
        apply(match, request);
        guardDuplicate(match);
        FootballMatch saved = matchRepository.save(match);
        refreshTable(saved);
        auditService.record("CREATE", "Match", saved.getId(), null, saved.getOpponent().getName(), null);
        return view(saved);
    }

    public Map<String, Object> update(Long id, MatchWrite request) {
        FootballMatch match = load(id);
        apply(match, request);
        guardDuplicate(match);
        recomputeScore(match);
        bonusService.sync(match);
        refreshTable(match);
        auditService.record("UPDATE", "Match", match.getId(), null, match.getStatus().name(), null);
        return view(match);
    }

    public void delete(Long id) {
        FootballMatch match = load(id);
        boolean lineup = !lineupRepository.findByMatchIdOrderByRevisionDesc(id).isEmpty();
        boolean result = match.getStatus() == MatchStatus.FINISHED || match.getGoalsFor() > 0 || match.getGoalsAgainst() > 0;
        if (lineup || result) {
            throw BusinessException.conflict("This match has a lineup or a result. Cancel the match instead of deleting it.", "MATCH_LOCKED", null);
        }
        matchRepository.delete(match);
        auditService.record("DELETE", "Match", id, match.getOpponent().getName(), null, null);
    }

    public Map<String, Object> reschedule(Long id, Instant kickoff) {
        FootballMatch match = load(id);
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw BusinessException.conflict("This match can no longer be moved", "MATCH_LOCKED", null);
        }
        match.setKickoff(kickoff);
        guardDuplicate(match);
        notificationService.notifyRoles(List.of(Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.ASSISTANT_MANAGER, Role.PRESIDENT, Role.GENERAL_MANAGER),
                "MATCH", "Match rescheduled", match.getOpponent().getName(), "/admin/calendar");
        auditService.record("RESCHEDULE", "Match", id, null, kickoff.toString(), null);
        return view(match);
    }

    public Map<String, Object> recordResult(Long id, int goalsFor, int goalsAgainst, Integer crowd, List<Scorer> scorers) {
        if (scorers == null || scorers.size() != goalsFor) {
            throw BusinessException.unprocessable("The score must match the number of goal events", "RESULT_MISMATCH");
        }
        FootballMatch match = load(id);
        for (MatchEvent event : new ArrayList<>(eventRepository.findByMatchIdOrderByEventMinuteAsc(id))) {
            if (event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL) {
                eventRepository.delete(event);
            }
        }
        for (Scorer scorer : scorers) {
            addEvent(id, new EventWrite(scorer.minute(), EventType.GOAL, scorer.playerId(), scorer.assistId(), null));
        }
        match.setGoalsAgainst(goalsAgainst);
        match.setCrowdAttendance(crowd);
        match.setStatus(MatchStatus.FINISHED);
        recomputeScore(match);
        bonusService.sync(match);
        refreshTable(match);
        notificationService.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER, Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.ASSISTANT_MANAGER),
                "RESULT", "Result: Viking FC " + match.getGoalsFor() + "-" + goalsAgainst + " " + match.getOpponent().getName(),
                "League table updated", "/admin/table");
        auditService.record("RESULT", "Match", id, null, match.getGoalsFor() + "-" + goalsAgainst, null);
        return view(match);
    }

    public Map<String, Object> addEvent(Long matchId, EventWrite request) {
        FootballMatch match = load(matchId);
        if (match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.POSTPONED) {
            throw BusinessException.conflict("Events cannot be added to a " + match.getStatus() + " match");
        }
        MatchEvent event = new MatchEvent();
        event.setMatch(match);
        event.setEventMinute(request.minute());
        event.setEventType(request.type());
        event.setPlayer(request.playerId() == null ? null : player(request.playerId()));
        event.setRelatedPlayer(request.relatedPlayerId() == null ? null : player(request.relatedPlayerId()));
        event.setNote(request.note());
        eventRepository.save(event);
        if (match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.LINEUP_PUBLISHED) {
            match.setStatus(MatchStatus.LIVE);
        }
        rebuildRecords(match);
        recomputeScore(match);
        bonusService.sync(match);
        return view(match);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> events(Long matchId) {
        load(matchId);
        return eventRepository.findByMatchIdOrderByEventMinuteAsc(matchId).stream().map(event -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", event.getId());
            body.put("minute", event.getEventMinute());
            body.put("type", event.getEventType());
            body.put("playerId", event.getPlayer() == null ? null : event.getPlayer().getId());
            body.put("playerName", event.getPlayer() == null ? null : event.getPlayer().getKnownAs());
            body.put("relatedPlayerId", event.getRelatedPlayer() == null ? null : event.getRelatedPlayer().getId());
            body.put("relatedPlayerName", event.getRelatedPlayer() == null ? null : event.getRelatedPlayer().getKnownAs());
            body.put("note", event.getNote());
            return body;
        }).toList();
    }

    public Map<String, Object> markLate(Long matchId, Long playerId, int minutesLate, boolean justified) {
        FootballMatch match = load(matchId);
        Player person = player(playerId);
        rebuildRecords(match);
        MatchPlayerRecord record = recordRepository.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseThrow(() -> BusinessException.unprocessable("Publish a lineup before recording a late arrival"));
        record.setLateToMatch(minutesLate > 0);
        record.setMinutesLate(minutesLate);
        fineService.matchLate(match, person, minutesLate, justified);
        return view(match);
    }

    public Map<String, Object> rate(Long matchId, Long playerId, BigDecimal rating) {
        if (rating == null || rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(BigDecimal.TEN) > 0) {
            throw BusinessException.unprocessable("Rating must be between 1 and 10");
        }
        MatchPlayerRecord record = recordRepository.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseThrow(() -> BusinessException.unprocessable("This player has no match record yet"));
        record.setRating(rating);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerId", playerId);
        body.put("rating", rating);
        return body;
    }

    public void rebuildRecords(FootballMatch match) {
        ClubSettings settings = settingsService.current();
        int length = settings.getMatchLengthMinutes() + match.getStoppageMinutes();
        Map<Long, BigDecimal> ratings = new HashMap<>();
        Map<Long, Integer> late = new HashMap<>();
        for (MatchPlayerRecord existing : recordRepository.findByMatchId(match.getId())) {
            if (existing.getRating() != null) {
                ratings.put(existing.getPlayer().getId(), existing.getRating());
            }
            if (existing.getMinutesLate() > 0) {
                late.put(existing.getPlayer().getId(), existing.getMinutesLate());
            }
        }
        recordRepository.deleteAll(recordRepository.findByMatchId(match.getId()));
        entityManager.flush();
        LineupVersion published = lineupRepository.findByMatchIdOrderByRevisionDesc(match.getId()).stream()
                .filter(version -> version.getStatus() == LineupState.PUBLISHED
                        || version.getStatus() == LineupState.LOCKED
                        || version.getStatus() == LineupState.FINISHED)
                .findFirst()
                .orElse(null);
        Map<Long, LineupSlot> slots = new HashMap<>();
        if (published != null) {
            for (LineupSlot slot : slotRepository.findByLineupIdOrderBySortOrderAsc(published.getId())) {
                slots.put(slot.getPlayer().getId(), slot);
            }
        }
        Map<Long, Integer> subIn = new HashMap<>();
        Map<Long, Integer> subOut = new HashMap<>();
        Map<Long, Integer> goals = new HashMap<>();
        Map<Long, Integer> assists = new HashMap<>();
        Map<Long, Integer> yellow = new HashMap<>();
        Map<Long, Integer> red = new HashMap<>();
        for (MatchEvent event : eventRepository.findByMatchIdOrderByEventMinuteAsc(match.getId())) {
            Long playerId = event.getPlayer() == null ? null : event.getPlayer().getId();
            if (playerId == null) {
                continue;
            }
            switch (event.getEventType()) {
                case GOAL, PENALTY_GOAL -> goals.merge(playerId, 1, Integer::sum);
                case ASSIST -> assists.merge(playerId, 1, Integer::sum);
                case YELLOW_CARD -> yellow.merge(playerId, 1, Integer::sum);
                case RED_CARD -> red.merge(playerId, 1, Integer::sum);
                case SUB_IN -> subIn.putIfAbsent(playerId, event.getEventMinute());
                case SUB_OUT -> subOut.putIfAbsent(playerId, event.getEventMinute());
                default -> {
                }
            }
            if ((event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL)
                    && event.getRelatedPlayer() != null) {
                assists.merge(event.getRelatedPlayer().getId(), 1, Integer::sum);
            }
        }
        for (Player person : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            LineupSlot slot = slots.get(person.getId());
            Participation participation = classify(person, slot, subIn.containsKey(person.getId()));
            int minutes = minutes(participation, person.getId(), length, subIn, subOut);
            boolean played = minutes > 0 || participation == Participation.STARTED || participation == Participation.SUB_USED;
            MatchPlayerRecord record = new MatchPlayerRecord();
            record.setMatch(match);
            record.setPlayer(person);
            record.setParticipation(participation);
            record.setMinutesPlayed(minutes);
            record.setRating(ratings.get(person.getId()));
            record.setGoals(goals.getOrDefault(person.getId(), 0));
            record.setAssists(assists.getOrDefault(person.getId(), 0));
            record.setYellowCards(yellow.getOrDefault(person.getId(), 0));
            record.setRedCards(red.getOrDefault(person.getId(), 0));
            int minutesLate = late.getOrDefault(person.getId(), 0);
            record.setMinutesLate(minutesLate);
            record.setLateToMatch(minutesLate > 0);
            record.setParticipated(played);
            recordRepository.save(record);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> records(Long matchId) {
        load(matchId);
        return recordRepository.findByMatchId(matchId).stream().map(record -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("playerId", record.getPlayer().getId());
            body.put("name", record.getPlayer().getKnownAs());
            body.put("jersey", record.getPlayer().getJerseyNumber());
            body.put("participation", record.getParticipation());
            body.put("minutes", record.getMinutesPlayed());
            body.put("goals", record.getGoals());
            body.put("assists", record.getAssists());
            body.put("yellow", record.getYellowCards());
            body.put("red", record.getRedCards());
            body.put("rating", record.getRating());
            body.put("participated", record.isParticipated());
            body.put("late", record.isLateToMatch());
            body.put("minutesLate", record.getMinutesLate());
            return body;
        }).toList();
    }

    public Map<String, Object> saveOpponent(OpponentWrite request) {
        Opponent opponent = opponentRepository.findByNameIgnoreCase(request.name().trim()).orElseGet(Opponent::new);
        opponent.setName(request.name().trim());
        opponent.setShortName(request.shortName());
        opponent.setCity(request.city());
        opponent = opponentRepository.save(opponent);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", opponent.getId());
        body.put("name", opponent.getName());
        body.put("shortName", opponent.getShortName());
        body.put("city", opponent.getCity());
        body.put("logoUrl", opponent.getLogoUrl());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> opponents() {
        return opponentRepository.findAllByOrderByNameAsc().stream().map(opponent -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", opponent.getId());
            body.put("name", opponent.getName());
            body.put("shortName", opponent.getShortName());
            body.put("city", opponent.getCity());
            body.put("logoUrl", opponent.getLogoUrl());
            return body;
        }).toList();
    }

    public List<Map<String, Object>> replaceStandings(List<StandingWrite> rows) {
        var season = settingsService.currentSeason();
        standingRepository.deleteAll(standingRepository.findBySeasonIdOrderByPositionNoAsc(season.getId()));
        entityManager.flush();
        for (StandingWrite row : rows) {
            Standing standing = new Standing();
            standing.setSeason(season);
            standing.setPositionNo(row.position());
            standing.setTeamName(row.teamName());
            standing.setPlayed(row.played());
            standing.setWon(row.won());
            standing.setDrawn(row.drawn());
            standing.setLost(row.lost());
            standing.setGoalsFor(row.goalsFor());
            standing.setGoalsAgainst(row.goalsAgainst());
            standing.setPoints(row.points());
            standingRepository.save(standing);
        }
        return standings();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> standings() {
        return standingRepository.findBySeasonIdOrderByPositionNoAsc(settingsService.currentSeason().getId()).stream()
                .map(row -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("position", row.getPositionNo());
                    body.put("team", row.getTeamName());
                    body.put("played", row.getPlayed());
                    body.put("won", row.getWon());
                    body.put("drawn", row.getDrawn());
                    body.put("lost", row.getLost());
                    body.put("goalsFor", row.getGoalsFor());
                    body.put("goalsAgainst", row.getGoalsAgainst());
                    body.put("points", row.getPoints());
                    return body;
                }).toList();
    }

    public FootballMatch load(Long id) {
        return matchRepository.findById(id).orElseThrow(() -> NotFoundException.of("Match", id));
    }

    private void apply(FootballMatch match, MatchWrite request) {
        match.setCompetition(request.competition());
        match.setRoundLabel(request.roundLabel());
        match.setMatchday(request.matchday());
        match.setOpponent(opponentRepository.findById(request.opponentId())
                .orElseThrow(() -> NotFoundException.of("Opponent", request.opponentId())));
        match.setKickoff(request.kickoff());
        match.setVenue(request.venue());
        match.setHomeMatch(Boolean.TRUE.equals(request.homeMatch()));
        if (request.status() != null) {
            match.setStatus(request.status());
        }
        if (request.goalsAgainst() != null) {
            match.setGoalsAgainst(request.goalsAgainst());
        }
        match.setCrowdAttendance(request.crowdAttendance());
        match.setReferee(request.referee());
        match.setNotes(request.notes());
        if (request.stoppageMinutes() != null) {
            match.setStoppageMinutes(request.stoppageMinutes());
        }
        linkCompetition(match, request);
        if (request.stage() != null) {
            match.setStage(request.stage());
        }
        if (request.alertHours() != null) {
            match.setAlertHours(request.alertHours());
        }
        if (request.squadSizeOverride() != null) {
            match.setSquadSizeOverride(request.squadSizeOverride() <= 0 ? null : request.squadSizeOverride());
        }
    }

    private void linkCompetition(FootballMatch match, MatchWrite request) {
        Competition competition = null;
        if (request.competitionId() != null) {
            competition = competitionRepository.findById(request.competitionId())
                    .orElseThrow(() -> NotFoundException.of("Competition", request.competitionId()));
            match.setCompetition(competition.getName());
        } else if (request.competition() != null) {
            competition = competitionRepository.findBySeasonIdAndNameIgnoreCase(match.getSeason().getId(), request.competition()).orElse(null);
            if (competition == null) {
                boolean friendly = "friendly".equalsIgnoreCase(request.competition()) || "amical".equalsIgnoreCase(request.competition());
                competition = new Competition();
                competition.setSeason(match.getSeason());
                competition.setName(request.competition());
                competition.setType(friendly ? CompetitionType.FRIENDLY : CompetitionType.OTHER);
                competition.setOfficial(!friendly);
                competition.setCountsForTable(false);
                competition.setColor(friendly ? "#5c6570" : "#a3283a");
                competition = competitionRepository.save(competition);
            }
        }
        if (competition == null) {
            return;
        }
        match.setCompetitionRef(competition);
        if (competition.getType() == CompetitionType.FRIENDLY) {
            match.setOfficial(false);
        } else if (request.official() != null) {
            match.setOfficial(request.official());
        } else {
            match.setOfficial(competition.isOfficial());
        }
    }

    private void guardDuplicate(FootballMatch match) {
        boolean duplicate = matchRepository.findBySeasonIdOrderByKickoffAsc(match.getSeason().getId()).stream()
                .anyMatch(other -> (match.getId() == null || !match.getId().equals(other.getId()))
                        && other.getStatus() != MatchStatus.CANCELLED
                        && other.getOpponent().getId().equals(match.getOpponent().getId())
                        && other.getCompetition().equalsIgnoreCase(match.getCompetition())
                        && Math.abs(Duration.between(other.getKickoff(), match.getKickoff()).toHours()) < 20);
        if (duplicate) {
            throw BusinessException.conflict("A match against this opponent in this competition already exists on this date", "DUPLICATE_MATCH", null);
        }
    }

    private void refreshTable(FootballMatch match) {
        if (match.getStatus() == MatchStatus.FINISHED && match.getCompetitionRef() != null && match.getCompetitionRef().isCountsForTable()) {
            standingsService.recalculate(match.getCompetitionRef().getId());
        }
    }

    private void recomputeScore(FootballMatch match) {
        int forUs = 0;
        int own = 0;
        for (MatchEvent event : eventRepository.findByMatchIdOrderByEventMinuteAsc(match.getId())) {
            if (event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL) {
                forUs++;
            }
            if (event.getEventType() == EventType.OWN_GOAL) {
                own++;
            }
        }
        match.setGoalsFor(forUs);
        if (own > match.getGoalsAgainst()) {
            match.setGoalsAgainst(own);
        }
    }

    private Participation classify(Player person, LineupSlot slot, boolean cameOn) {
        if (slot != null && slot.getSelection() == Selection.STARTING) {
            return Participation.STARTED;
        }
        if (slot != null && slot.getSelection() == Selection.SUBSTITUTE) {
            return cameOn ? Participation.SUB_USED : Participation.SUB_UNUSED;
        }
        if (person.getStatus() == PlayerStatus.INJURED) {
            return Participation.INJURED;
        }
        if (person.getStatus() == PlayerStatus.SUSPENDED) {
            return Participation.SUSPENDED;
        }
        return Participation.NOT_SELECTED;
    }

    private int minutes(Participation participation, Long playerId, int length,
                        Map<Long, Integer> subIn, Map<Long, Integer> subOut) {
        if (participation == Participation.STARTED) {
            return Math.max(0, subOut.getOrDefault(playerId, length));
        }
        if (participation == Participation.SUB_USED) {
            int start = subIn.getOrDefault(playerId, 0);
            return Math.max(0, subOut.getOrDefault(playerId, length) - start);
        }
        return 0;
    }

    private Player player(Long id) {
        return playerRepository.findByIdAndActiveTrue(id).orElseThrow(() -> NotFoundException.of("Player", id));
    }

    private Map<String, Object> view(FootballMatch match) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", match.getId());
        body.put("competition", match.getCompetition());
        body.put("round", match.getRoundLabel());
        body.put("matchday", match.getMatchday());
        body.put("opponentId", match.getOpponent().getId());
        body.put("opponent", match.getOpponent().getName());
        body.put("opponentLogo", match.getOpponent().getLogoUrl());
        body.put("kickoff", match.getKickoff());
        body.put("kickoffDate", match.getKickoff().atZone(JpaConfig.KIGALI).toLocalDate());
        body.put("venue", match.getVenue());
        body.put("home", match.isHomeMatch());
        body.put("status", match.getStatus());
        body.put("goalsFor", match.getGoalsFor());
        body.put("goalsAgainst", match.getGoalsAgainst());
        body.put("crowd", match.getCrowdAttendance());
        body.put("referee", match.getReferee());
        body.put("formation", match.getFormation());
        body.put("lineupVisible", lineupVisible(match));
        body.put("stage", match.getStage());
        body.put("official", match.isOfficial());
        body.put("squadSizeOverride", match.getSquadSizeOverride());
        body.put("alertHours", match.getAlertHours());
        if (match.getCompetitionRef() != null) {
            body.put("competitionId", match.getCompetitionRef().getId());
            body.put("competitionType", match.getCompetitionRef().getType());
            body.put("competitionColor", match.getCompetitionRef().getColor());
            body.put("countsForTable", match.getCompetitionRef().isCountsForTable());
        }
        LineupVersion lineup = lineupRepository.findByMatchIdOrderByRevisionDesc(match.getId()).stream().findFirst().orElse(null);
        body.put("lineupStatus", lineup == null ? "NONE" : lineup.getStatus().name());
        List<Convocation> calls = match.getId() == null ? List.of() : convocationRepository.findByMatchId(match.getId());
        body.put("calledCount", calls.stream().filter(Convocation::isCalled).count());
        body.put("confirmedCount", calls.stream().filter(call -> call.isCalled() && "CONFIRMED".equals(call.getResponse())).count());
        body.put("alerts", matchAlerts(match, lineup));
        return body;
    }

    private List<String> matchAlerts(FootballMatch match, LineupVersion lineup) {
        List<String> alerts = new ArrayList<>();
        if (match.getKickoff() == null) {
            return alerts;
        }
        Instant now = Instant.now();
        boolean published = lineup != null && (lineup.getStatus() == rw.vikingfc.club.lineup.LineupState.PUBLISHED
                || lineup.getStatus() == rw.vikingfc.club.lineup.LineupState.LOCKED
                || lineup.getStatus() == rw.vikingfc.club.lineup.LineupState.FINISHED);
        if ((match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.LINEUP_PUBLISHED) && match.getKickoff().isBefore(now)) {
            alerts.add("RESULT_MISSING");
        }
        if (!published && match.getStatus() != MatchStatus.CANCELLED && match.getStatus() != MatchStatus.POSTPONED && match.getStatus() != MatchStatus.FINISHED) {
            long hours = Duration.between(now, match.getKickoff()).toHours();
            if (hours >= 0 && hours <= 72) {
                alerts.add(hours < match.getAlertHours() ? "LINEUP_OVERDUE" : "LINEUP_DUE");
            }
        }
        return alerts;
    }

    private boolean lineupVisible(FootballMatch match) {
        if (!ClubAccess.publishedOnly()) {
            return true;
        }
        return match.getStatus() == MatchStatus.LINEUP_PUBLISHED
                || match.getStatus() == MatchStatus.LIVE
                || match.getStatus() == MatchStatus.FINISHED;
    }

    public record MatchWrite(@NotNull String competition, String roundLabel, Integer matchday, @NotNull Long opponentId,
                             @NotNull Instant kickoff, String venue, Boolean homeMatch, MatchStatus status,
                             Integer goalsAgainst, Integer crowdAttendance, String referee, String notes,
                             Integer stoppageMinutes, Long competitionId, String stage, Boolean official, Integer alertHours,
                             Integer squadSizeOverride) {
    }

    public record Scorer(int minute, Long playerId, Long assistId) {
    }

    public record EventWrite(@NotNull Integer minute, @NotNull EventType type, Long playerId, Long relatedPlayerId, String note) {
    }

    public record OpponentWrite(@NotNull String name, String shortName, String city) {
    }

    public record StandingWrite(@NotNull Integer position, @NotNull String teamName, int played, int won, int drawn,
                                int lost, int goalsFor, int goalsAgainst, int points) {
    }
}
