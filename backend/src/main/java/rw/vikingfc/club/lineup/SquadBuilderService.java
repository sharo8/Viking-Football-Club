package rw.vikingfc.club.lineup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceMark;
import rw.vikingfc.club.attendance.AttendanceRecordRepository;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.match.EventType;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.match.MatchEvent;
import rw.vikingfc.club.match.MatchEventRepository;
import rw.vikingfc.club.match.MatchPlayerRecord;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.match.MatchService;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.medical.Injury;
import rw.vikingfc.club.medical.InjuryRepository;
import rw.vikingfc.club.medical.InjuryStatus;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SquadBuilderService {

    private final LineupCenterService center;
    private final MatchService matchService;
    private final LineupVersionRepository lineupRepository;
    private final LineupSlotRepository slotRepository;
    private final LineupSquadMemberRepository memberRepository;
    private final PlayerRepository playerRepository;
    private final InjuryRepository injuryRepository;
    private final ConvocationRepository convocationRepository;
    private final MatchRatingRepository ratingRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final FootballMatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final SettingsService settingsService;
    private final CompetitionSquadRuleRepository competitionRuleRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void saveSize(Long matchId, SizeWrite request) {
        ClubSettings settings = settingsService.current();
        FootballMatch match = matchService.load(matchId);
        int maxBench = ruleBench(match, settings);
        int size = request.squadSize() == null ? settings.getLineupDefaultSquadSize() : request.squadSize();
        int competitionMax = competitionMax(match, settings);
        String gate = SquadRules.sizeGate(size, settings.getLineupMinSquad(), settings.getLineupMaxSquad(), competitionMax, SecurityUtils.isExecutive(), request.overrideReason());
        if (gate != null || size - 11 > maxBench) {
            throw BusinessException.unprocessable("Squad size must stay inside the competition rule", gate == null ? "SQUAD_SIZE_INVALID" : gate);
        }
        Map<String, Integer> plan = request.plan() == null || request.plan().isEmpty() ? SquadRules.recommended(size) : normalize(request.plan());
        List<String> planIssues = SquadRules.planIssues(size, plan);
        if (!planIssues.isEmpty()) {
            throw BusinessException.unprocessable("The line quotas must add up to the squad size", planIssues.get(0));
        }
        LineupVersion lineup = touch(match, request.version());
        lineup.setSquadSize(size);
        lineup.setSquadPlanJson(write(plan));
        match.setSquadSizeOverride(size);
        if (Boolean.TRUE.equals(request.useAsDefault())) {
            if (!SecurityUtils.isExecutive()) {
                throw BusinessException.forbidden("Only an executive can change the competition default");
            }
            CompetitionSquadRule rule = competitionRuleRepository.findByCompetitionIgnoreCase(match.getCompetition() == null ? "" : match.getCompetition())
                    .orElseGet(CompetitionSquadRule::new);
            rule.setCompetition(match.getCompetition());
            rule.setSquadSize(size);
            rule.setMaxBench(maxBench);
            if (rule.getMinGoalkeepers() < 1) {
                rule.setMinGoalkeepers(2);
            }
            competitionRuleRepository.save(rule);
        }
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, "squad-size " + size, null);
    }

    private int competitionMax(FootballMatch match, ClubSettings settings) {
        return competitionRuleRepository.findByCompetitionIgnoreCase(match.getCompetition() == null ? "" : match.getCompetition())
                .map(CompetitionSquadRule::getSquadSize)
                .orElse(settings.getLineupDefaultSquadSize() == 0 ? 18 : settings.getLineupDefaultSquadSize());
    }

    public void saveSquad(Long matchId, SquadWrite request) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = touch(match, request.version());
        int size = lineup.getSquadSize() == null ? settingsService.current().getLineupDefaultSquadSize() : lineup.getSquadSize();
        List<MemberWrite> rows = request.players() == null ? List.of() : request.players();
        List<MemberWrite> called = rows.stream().filter(MemberWrite::called).toList();
        if (called.size() > size) {
            throw BusinessException.unprocessable("Squad is full", "SQUAD_SIZE_INVALID");
        }
        Set<Long> seen = new HashSet<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        SquadRules.LINES.forEach(line -> counts.put(line, 0));
        Availability availability = availability();
        boolean executive = SecurityUtils.isExecutive();
        for (MemberWrite row : called) {
            if (!seen.add(row.playerId())) {
                throw BusinessException.unprocessable("A player cannot be selected twice", "SQUAD_SIZE_INVALID");
            }
            Player player = playerRepository.findByIdAndActiveTrue(row.playerId()).orElseThrow(() -> NotFoundException.of("Player", row.playerId()));
            String state = availability.of(player);
            boolean blocked = "INJURED".equals(state) || "SUSPENDED".equals(state) || "UNAVAILABLE".equals(state);
            if (blocked && (!executive || row.overrideReason() == null || row.overrideReason().isBlank())) {
                throw BusinessException.unprocessable(player.getKnownAs() + " is unavailable", "PLAYER_UNAVAILABLE");
            }
            String line = row.line() == null || row.line().isBlank() ? SquadRules.lineOf(player.getLineName(), player.getPosition()) : row.line();
            counts.merge(line, 1, Integer::sum);
        }
        Map<String, Integer> plan = readPlan(lineup.getSquadPlanJson());
        if (plan != null) {
            for (String line : SquadRules.LINES) {
                if (counts.getOrDefault(line, 0) > plan.getOrDefault(line, 0)) {
                    throw BusinessException.unprocessable("That line is over its quota", "SQUAD_QUOTA_VIOLATION");
                }
            }
        }
        replaceMembers(match, lineup, rows);
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, "squad " + called.size(), null);
    }

    public void saveXi(Long matchId, XiWrite request) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = touch(match, request.version());
        if (request.formation() != null && !request.formation().isBlank()) {
            if (!FormationBook.known(request.formation())) {
                throw BusinessException.badRequest("Unknown formation");
            }
            lineup.setFormation(request.formation());
            match.setFormation(request.formation());
        }
        if (request.opponentFormation() != null) {
            lineup.setOpponentFormation(request.opponentFormation());
        }
        List<XiSlot> slots = request.slots() == null ? List.of() : request.slots();
        if (slots.size() > 11) {
            throw BusinessException.unprocessable("A lineup needs exactly 11 starters", "XI_NOT_11");
        }
        Set<Long> called = calledIds(lineup.getId());
        Set<Long> seen = new HashSet<>();
        Availability availability = availability();
        boolean executive = SecurityUtils.isExecutive();
        List<Player> players = new ArrayList<>();
        for (XiSlot slot : slots) {
            if (!seen.add(slot.playerId())) {
                throw BusinessException.unprocessable("A player cannot appear twice in the lineup", "XI_NOT_11");
            }
            if (!called.isEmpty() && !called.contains(slot.playerId())) {
                throw BusinessException.unprocessable("Only called-up players can start", "PLAYER_NOT_CALLED_UP");
            }
            Player player = playerRepository.findByIdAndActiveTrue(slot.playerId()).orElseThrow(() -> NotFoundException.of("Player", slot.playerId()));
            String state = availability.of(player);
            boolean blocked = "INJURED".equals(state) || "SUSPENDED".equals(state) || "UNAVAILABLE".equals(state);
            if (blocked && (!executive || slot.overrideReason() == null || slot.overrideReason().isBlank())) {
                throw BusinessException.unprocessable(player.getKnownAs() + " is unavailable", "PLAYER_UNAVAILABLE");
            }
            if ("GK".equals(slot.positionSlot()) && player.getPosition() != Position.GOALKEEPER) {
                throw BusinessException.unprocessable("The goalkeeper slot has to be a goalkeeper", "MIN_GOALKEEPERS");
            }
            players.add(player);
        }
        slotRepository.deleteByLineupId(lineup.getId());
        entityManager.flush();
        int order = 1;
        Set<Long> starters = new HashSet<>();
        for (XiSlot slot : slots) {
            Player player = players.stream().filter(item -> item.getId().equals(slot.playerId())).findFirst().orElseThrow();
            LineupSlot row = new LineupSlot();
            row.setLineup(lineup);
            row.setPlayer(player);
            row.setSelection(Selection.STARTING);
            row.setPositionSlot(slot.positionSlot());
            row.setSortOrder(order++);
            row.setCaptain(Boolean.TRUE.equals(slot.captain()));
            row.setOverrideReason(slot.overrideReason());
            row.setCoordX(slot.x());
            row.setCoordY(slot.y());
            slotRepository.save(row);
            starters.add(player.getId());
        }
        List<LineupSquadMember> members = memberRepository.findByLineupIdOrderByBenchOrderAsc(lineup.getId());
        int bench = 0;
        for (LineupSquadMember member : members.stream().filter(LineupSquadMember::isCalled).sorted(Comparator.comparing(member -> member.getBenchOrder() == null ? 99 : member.getBenchOrder())).toList()) {
            if (starters.contains(member.getPlayer().getId())) {
                member.setBenchOrder(null);
                continue;
            }
            LineupSlot row = new LineupSlot();
            row.setLineup(lineup);
            row.setPlayer(member.getPlayer());
            row.setSelection(Selection.SUBSTITUTE);
            row.setPositionSlot("BENCH");
            row.setSortOrder(20 + bench);
            row.setCaptain(false);
            member.setBenchOrder(bench);
            bench++;
            slotRepository.save(row);
        }
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, lineup.getFormation(), null);
    }

    public void suggest(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = touch(match, null);
        int size = lineup.getSquadSize() == null ? settingsService.current().getLineupDefaultSquadSize() : lineup.getSquadSize();
        if (lineup.getSquadPlanJson() == null) {
            lineup.setSquadSize(size);
            lineup.setSquadPlanJson(write(SquadRules.recommended(size)));
        }
        Map<String, Integer> plan = readPlan(lineup.getSquadPlanJson());
        List<Ranked> ranked = ranked(match);
        List<MemberWrite> chosen = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (String line : SquadRules.LINES) {
            int quota = plan.getOrDefault(line, 0);
            int taken = 0;
            for (Ranked row : ranked) {
                if (taken >= quota) {
                    break;
                }
                if (!line.equals(row.line()) || !row.available() || !used.add(row.player().getId())) {
                    continue;
                }
                chosen.add(new MemberWrite(row.player().getId(), line, true, null, null, null));
                taken++;
            }
        }
        for (Ranked row : ranked) {
            if (used.contains(row.player().getId())) {
                continue;
            }
            chosen.add(new MemberWrite(row.player().getId(), row.line(), false, null, row.available() ? "Tactical" : reasonFor(row.state()), null));
        }
        replaceMembers(match, lineup, chosen);
        slotRepository.deleteByLineupId(lineup.getId());
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, "suggest-squad", null);
    }

    public void autoXi(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = touch(match, null);
        String formation = lineup.getFormation() == null ? "4-3-3" : lineup.getFormation();
        List<Ranked> ranked = ranked(match);
        Set<Long> called = calledIds(lineup.getId());
        List<Ranked> pool = ranked.stream().filter(row -> called.contains(row.player().getId()) && row.available()).toList();
        Set<Long> used = new HashSet<>();
        List<XiSlot> slots = new ArrayList<>();
        for (FormationBook.Spot spot : FormationBook.spots(formation)) {
            Ranked pick = pool.stream().filter(row -> !used.contains(row.player().getId()) && SquadRules.fits(spot.code(), row.player().getPosition())).findFirst().orElse(null);
            if (pick == null && !"GK".equals(spot.code())) {
                pick = pool.stream().filter(row -> !used.contains(row.player().getId()) && row.player().getPosition() != Position.GOALKEEPER).findFirst().orElse(null);
            }
            if (pick == null) {
                continue;
            }
            used.add(pick.player().getId());
            slots.add(new XiSlot(pick.player().getId(), spot.code(), spot.x(), spot.y(), false, null));
        }
        saveXi(matchId, new XiWrite(formation, slots, null, lineup.getOpponentFormation()));
    }

    public Map<String, Object> edit(Long matchId) {
        requireEditor();
        FootballMatch match = matchService.load(matchId);
        LineupVersion latest = latest(matchId);
        if (latest != null && locked(latest.getStatus()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.unprocessable("The lineup is locked", "LINEUP_LOCKED");
        }
        if (latest == null || latest.getStatus() == LineupState.DRAFT || latest.getStatus() == LineupState.CHANGES_REQUESTED) {
            return center.sheet(matchId);
        }
        fork(match, latest);
        return center.sheet(matchId);
    }

    public void copyPrevious(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = touch(match, null);
        FootballMatch previous = matchRepository.findBySeasonIdOrderByKickoffAsc(match.getSeason().getId()).stream()
                .filter(item -> item.getKickoff().isBefore(match.getKickoff()))
                .filter(item -> latest(item.getId()) != null)
                .reduce((a, b) -> b)
                .orElseThrow(() -> BusinessException.unprocessable("No previous lineup to copy"));
        LineupVersion source = latest(previous.getId());
        lineup.setFormation(source.getFormation());
        lineup.setSquadSize(source.getSquadSize());
        lineup.setSquadPlanJson(source.getSquadPlanJson());
        lineup.setOpponentFormation(source.getOpponentFormation());
        copyMembers(source, lineup);
        copySlots(source, lineup);
        syncCalls(match, memberRepository.findByLineupIdOrderByBenchOrderAsc(lineup.getId()));
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, "copy-previous", null);
    }

    public void duplicate(Long matchId, Long targetMatchId) {
        if (targetMatchId == null || targetMatchId.equals(matchId)) {
            throw BusinessException.badRequest("Choose another match");
        }
        FootballMatch sourceMatch = matchService.load(matchId);
        FootballMatch target = matchService.load(targetMatchId);
        LineupVersion source = latest(sourceMatch.getId());
        if (source == null) {
            throw BusinessException.unprocessable("This match has no lineup to copy");
        }
        LineupVersion existing = latest(target.getId());
        LineupVersion lineup;
        if (existing == null || existing.getStatus() == LineupState.DRAFT || existing.getStatus() == LineupState.CHANGES_REQUESTED) {
            lineup = touch(target, null);
        } else if (locked(existing.getStatus()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.unprocessable("The lineup is locked", "LINEUP_LOCKED");
        } else {
            lineup = fork(target, existing);
        }
        lineup.setFormation(source.getFormation());
        lineup.setSquadSize(source.getSquadSize());
        lineup.setSquadPlanJson(source.getSquadPlanJson());
        copyMembers(source, lineup);
        copySlots(source, lineup);
        syncCalls(target, memberRepository.findByLineupIdOrderByBenchOrderAsc(lineup.getId()));
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, "duplicate", String.valueOf(matchId));
    }

    public Map<String, Object> confirm(Long matchId, ConfirmWrite request) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = latest(matchId);
        if (lineup == null) {
            throw BusinessException.unprocessable("Build a lineup before confirming it", "XI_NOT_11");
        }
        if (locked(lineup.getStatus()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.unprocessable("The lineup is locked", "LINEUP_LOCKED");
        }
        boolean executive = SecurityUtils.isExecutive();
        boolean head = executive || SecurityUtils.hasRole("HEAD_COACH");
        if (!head) {
            throw BusinessException.forbidden("Only the head coach can confirm the lineup");
        }
        if (request.meetingAt() != null) {
            lineup.setMeetingAt(request.meetingAt());
        }
        if (request.meetingPlace() != null) {
            lineup.setMeetingPlace(request.meetingPlace());
        }
        if (request.comment() != null) {
            lineup.setReviewComment(request.comment());
        }
        Instant deadline = lineup.getDeadlineAt() == null ? match.getKickoff().minusSeconds(settingsService.current().getLineupDeadlineHours() * 3600L) : lineup.getDeadlineAt();
        boolean late = Instant.now(clock).isAfter(deadline);
        if (late && !executive) {
            throw BusinessException.unprocessable("The 48-hour deadline has passed", "DEADLINE_PASSED");
        }
        if (late && executive && (request.overrideReason() == null || request.overrideReason().isBlank())) {
            throw BusinessException.unprocessable("An override needs a written reason", "DEADLINE_PASSED");
        }
        LineupState status = lineup.getStatus();
        if (status == LineupState.DRAFT || status == LineupState.CHANGES_REQUESTED) {
            center.transition(matchId, "submit", request.comment());
            status = LineupState.SUBMITTED;
        }
        if (executive) {
            if (status == LineupState.SUBMITTED) {
                center.transition(matchId, "approve", request.comment());
            }
            LineupVersion current = latest(matchId);
            if (current.getStatus() != LineupState.PUBLISHED && current.getStatus() != LineupState.LOCKED && current.getStatus() != LineupState.FINISHED) {
                String reason = request.overrideReason() == null || request.overrideReason().isBlank() ? request.comment() : request.overrideReason();
                center.transition(matchId, "publish", reason);
            }
            if (late) {
                latest(matchId).setOverrideReason(request.overrideReason());
            }
        }
        auditService.record("CONFIRM", "Lineup", latest(matchId).getId(), null, latest(matchId).getStatus().name(), request.comment());
        return center.sheet(matchId);
    }

    public Map<String, Object> overview(int page, int size, String status, String competition, Boolean home, String query) {
        ClubSettings settings = settingsService.current();
        List<FootballMatch> matches = new ArrayList<>(matchRepository.findBySeasonIdOrderByKickoffAsc(settings.getSeason().getId()));
        matches.sort(Comparator.comparing(FootballMatch::getKickoff).reversed());
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FootballMatch match : matches) {
            if (competition != null && !competition.isBlank() && !competition.equalsIgnoreCase(match.getCompetition())) {
                continue;
            }
            if (home != null && match.isHomeMatch() != home) {
                continue;
            }
            if (!needle.isEmpty() && !match.getOpponent().getName().toLowerCase(Locale.ROOT).contains(needle)
                    && !match.getCompetition().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            LineupVersion lineup = latest(match.getId());
            String lineupStatus = lineup == null ? "NONE" : lineup.getStatus().name();
            if (ClubAccess.publishedOnly() && (lineup == null || !visible(lineup.getStatus()))) {
                continue;
            }
            if (status != null && !status.isBlank() && !"ANY".equalsIgnoreCase(status)) {
                boolean matchStatus = status.equalsIgnoreCase(match.getStatus().name());
                boolean sheetStatus = status.equalsIgnoreCase(lineupStatus);
                if (!matchStatus && !sheetStatus) {
                    continue;
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("matchId", match.getId());
            row.put("kickoff", match.getKickoff());
            row.put("opponent", match.getOpponent().getName());
            row.put("opponentShort", match.getOpponent().getShortName());
            row.put("opponentLogo", match.getOpponent().getLogoUrl());
            row.put("competition", match.getCompetition());
            row.put("home", match.isHomeMatch());
            row.put("venue", match.getVenue());
            row.put("matchStatus", match.getStatus());
            row.put("goalsFor", match.getGoalsFor());
            row.put("goalsAgainst", match.getGoalsAgainst());
            row.put("lineupStatus", lineupStatus);
            row.put("squadSize", lineup == null || lineup.getSquadSize() == null ? null : lineup.getSquadSize());
            row.put("formation", lineup == null ? match.getFormation() : lineup.getFormation());
            row.put("revision", lineup == null ? null : lineup.getRevision());
            rows.add(row);
        }
        int pageSize = size <= 0 ? 8 : Math.min(size, 50);
        int from = Math.max(page, 0) * pageSize;
        int to = Math.min(rows.size(), from + pageSize);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", Math.max(page, 0));
        body.put("size", pageSize);
        body.put("total", rows.size());
        body.put("rows", from >= rows.size() ? List.of() : rows.subList(from, to));
        return body;
    }

    public Map<String, Object> participation(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = latest(matchId);
        List<LineupSlot> slots = lineup == null ? List.of() : slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId());
        List<LineupSquadMember> members = lineup == null ? List.of() : memberRepository.findByLineupIdOrderByBenchOrderAsc(lineup.getId());
        List<MatchEvent> events = eventRepository.findByMatchIdOrderByEventMinuteAsc(matchId);
        Map<Long, MatchPlayerRecord> records = new HashMap<>();
        recordRepository.findByMatchId(matchId).forEach(record -> records.put(record.getPlayer().getId(), record));
        Map<Long, MatchRating> ratings = new HashMap<>();
        ratingRepository.findByMatchId(matchId).forEach(rating -> ratings.put(rating.getPlayer().getId(), rating));
        boolean live = match.getStatus() == MatchStatus.LIVE || match.getStatus() == MatchStatus.FINISHED;
        boolean minutesKnown = records.values().stream().anyMatch(record -> record.getMinutesPlayed() > 0);
        Set<Long> starters = new HashSet<>();
        slots.stream().filter(slot -> slot.getSelection() == Selection.STARTING).forEach(slot -> starters.add(slot.getPlayer().getId()));
        Set<Long> seen = new HashSet<>();
        List<Map<String, Object>> played = new ArrayList<>();
        List<Map<String, Object>> absent = new ArrayList<>();
        List<Player> people = new ArrayList<>();
        if (!members.isEmpty()) {
            members.forEach(member -> people.add(member.getPlayer()));
        } else {
            slots.forEach(slot -> people.add(slot.getPlayer()));
        }
        playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                .filter(player -> people.stream().noneMatch(item -> item.getId().equals(player.getId())))
                .forEach(people::add);
        for (Player player : people) {
            if (!seen.add(player.getId())) {
                continue;
            }
            MatchPlayerRecord record = records.get(player.getId());
            int minutes = record == null ? 0 : record.getMinutesPlayed();
            Integer subIn = events.stream().filter(event -> event.getEventType() == EventType.SUB_IN && event.getPlayer() != null && event.getPlayer().getId().equals(player.getId()))
                    .map(MatchEvent::getEventMinute).findFirst().orElse(null);
            Integer subOut = events.stream().filter(event -> event.getEventType() == EventType.SUB_OUT && event.getPlayer() != null && event.getPlayer().getId().equals(player.getId()))
                    .map(MatchEvent::getEventMinute).findFirst().orElse(null);
            boolean starter = starters.contains(player.getId());
            boolean onPitch = MatchdayLists.played(starter, minutes, subIn != null, live, minutesKnown);
            LineupSquadMember member = members.stream().filter(item -> item.getPlayer().getId().equals(player.getId())).findFirst().orElse(null);
            boolean called = member == null ? slots.stream().anyMatch(slot -> slot.getPlayer().getId().equals(player.getId())) : member.isCalled();
            if (!called && !onPitch && live) {
                absent.add(person(player, record, ratings.get(player.getId()), events, subIn, subOut, member == null ? "Tactical" : member.getReasonNotCalled(), false));
                continue;
            }
            if (!live && !starter && !called) {
                absent.add(person(player, record, ratings.get(player.getId()), events, subIn, subOut, member == null ? "Tactical" : member.getReasonNotCalled(), true));
                continue;
            }
            if (onPitch) {
                played.add(person(player, record, ratings.get(player.getId()), events, subIn, subOut, null, !live));
            } else if (called || starter) {
                absent.add(person(player, record, ratings.get(player.getId()), events, subIn, subOut, called ? "Unused" : "Tactical", !live));
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matchId", match.getId());
        body.put("opponent", match.getOpponent().getName());
        body.put("kickoff", match.getKickoff());
        body.put("matchStatus", match.getStatus());
        body.put("goalsFor", match.getGoalsFor());
        body.put("goalsAgainst", match.getGoalsAgainst());
        body.put("formation", lineup == null ? match.getFormation() : lineup.getFormation());
        body.put("squadSize", lineup == null ? null : lineup.getSquadSize());
        body.put("planned", !live);
        body.put("played", played);
        body.put("absent", absent);
        return body;
    }

    private Map<String, Object> person(Player player, MatchPlayerRecord record, MatchRating rating, List<MatchEvent> events,
                                       Integer subIn, Integer subOut, String reason, boolean planned) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", player.getId());
        row.put("knownAs", player.getKnownAs());
        row.put("jersey", player.getJerseyNumber());
        row.put("photoUrl", player.getPhotoUrl());
        row.put("position", player.getPosition().name());
        row.put("positionLabel", player.getPosition().getLabel());
        row.put("line", player.getLineName());
        row.put("lineCode", SquadRules.lineOf(player.getLineName(), player.getPosition()));
        row.put("minutes", record == null ? 0 : record.getMinutesPlayed());
        row.put("goals", events.stream().filter(event -> (event.getEventType() == EventType.GOAL || event.getEventType() == EventType.PENALTY_GOAL) && event.getPlayer() != null && event.getPlayer().getId().equals(player.getId())).count());
        row.put("assists", events.stream().filter(event -> event.getEventType() == EventType.GOAL && event.getRelatedPlayer() != null && event.getRelatedPlayer().getId().equals(player.getId())).count());
        row.put("yellow", events.stream().filter(event -> event.getEventType() == EventType.YELLOW_CARD && event.getPlayer() != null && event.getPlayer().getId().equals(player.getId())).count());
        row.put("red", events.stream().filter(event -> event.getEventType() == EventType.RED_CARD && event.getPlayer() != null && event.getPlayer().getId().equals(player.getId())).count());
        row.put("entered", subIn);
        row.put("replaced", subOut);
        row.put("rating", rating == null ? null : rating.getFinalRating());
        row.put("motm", rating != null && rating.isMotm());
        row.put("reason", reason);
        row.put("planned", planned);
        return row;
    }

    private void replaceMembers(FootballMatch match, LineupVersion lineup, List<MemberWrite> rows) {
        memberRepository.deleteByLineupId(lineup.getId());
        entityManager.flush();
        List<LineupSquadMember> stored = new ArrayList<>();
        int order = 0;
        for (MemberWrite row : rows) {
            Player player = playerRepository.findByIdAndActiveTrue(row.playerId()).orElseThrow(() -> NotFoundException.of("Player", row.playerId()));
            LineupSquadMember member = new LineupSquadMember();
            member.setLineup(lineup);
            member.setPlayer(player);
            member.setLineCode(row.line() == null || row.line().isBlank() ? SquadRules.lineOf(player.getLineName(), player.getPosition()) : row.line());
            member.setCalled(row.called());
            member.setBenchOrder(row.called() ? order++ : null);
            member.setReasonNotCalled(row.called() ? null : row.reasonNotCalled());
            member.setOverrideReason(row.overrideReason());
            memberRepository.save(member);
            stored.add(member);
        }
        syncCalls(match, stored);
    }

    private void syncCalls(FootballMatch match, List<LineupSquadMember> members) {
        for (LineupSquadMember member : members) {
            Convocation call = convocationRepository.findByMatchIdAndPlayerId(match.getId(), member.getPlayer().getId()).orElseGet(Convocation::new);
            call.setMatch(match);
            call.setPlayer(member.getPlayer());
            call.setCalled(member.isCalled());
            if (!member.isCalled()) {
                call.setReason(member.getReasonNotCalled());
                call.setResponse("PENDING");
            }
            if (call.getResponse() == null) {
                call.setResponse("PENDING");
            }
            convocationRepository.save(call);
        }
    }

    private LineupVersion touch(FootballMatch match, Long version) {
        requireEditor();
        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw BusinessException.conflict("A cancelled match has no lineup");
        }
        LineupVersion lineup = latest(match.getId());
        if (lineup != null && locked(lineup.getStatus()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.unprocessable("The lineup is locked", "LINEUP_LOCKED");
        }
        if (lineup != null && lineup.getEditingUntil() != null && lineup.getEditingUntil().isAfter(Instant.now(clock))
                && lineup.getEditingBy() != null && !lineup.getEditingBy().equals(SecurityUtils.email()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.conflict("Editing now: " + lineup.getEditingBy(), "EDIT_CONFLICT", lineup.getEditingBy());
        }
        if (lineup == null) {
            lineup = new LineupVersion();
            lineup.setMatch(match);
            lineup.setRevision(1);
            lineup.setStatus(LineupState.DRAFT);
            lineup.setFormation("4-3-3");
            lineup.setSquadSize(settingsService.current().getLineupDefaultSquadSize());
            lineup.setSquadPlanJson(write(SquadRules.recommended(lineup.getSquadSize())));
            lineup.setDeadlineAt(match.getKickoff().minusSeconds(settingsService.current().getLineupDeadlineHours() * 3600L));
            lineupRepository.save(lineup);
        } else if (lineup.getStatus() != LineupState.DRAFT && lineup.getStatus() != LineupState.CHANGES_REQUESTED) {
            throw BusinessException.unprocessable("Edit the lineup to open a new version", "INVALID_TRANSITION");
        }
        if (lineup.getId() != null && version != null && !version.equals(lineup.getVersion())) {
            throw BusinessException.conflict("Someone else saved this lineup. Reload it and try again.", "EDIT_CONFLICT", null);
        }
        lineup.setEditingBy(SecurityUtils.email());
        lineup.setEditingUntil(Instant.now(clock).plusSeconds(90));
        return lineup;
    }

    private LineupVersion fork(FootballMatch match, LineupVersion source) {
        int next = source.getRevision() + 1;
        LineupVersion copy = new LineupVersion();
        copy.setMatch(match);
        copy.setRevision(next);
        copy.setStatus(LineupState.DRAFT);
        copy.setFormation(source.getFormation());
        copy.setSquadSize(source.getSquadSize());
        copy.setSquadPlanJson(source.getSquadPlanJson());
        copy.setOpponentFormation(source.getOpponentFormation());
        copy.setOpponentNames(source.getOpponentNames());
        copy.setMeetingAt(source.getMeetingAt());
        copy.setMeetingPlace(source.getMeetingPlace());
        copy.setKitNote(source.getKitNote());
        copy.setTravelNote(source.getTravelNote());
        copy.setCallupMessage(source.getCallupMessage());
        copy.setDeadlineAt(source.getDeadlineAt());
        lineupRepository.save(copy);
        entityManager.flush();
        copyMembers(source, copy);
        copySlots(source, copy);
        auditService.record("UPDATE", "Lineup", copy.getId(), null, "version " + next, null);
        return copy;
    }

    private void copyMembers(LineupVersion source, LineupVersion target) {
        memberRepository.deleteByLineupId(target.getId());
        entityManager.flush();
        for (LineupSquadMember member : memberRepository.findByLineupIdOrderByBenchOrderAsc(source.getId())) {
            LineupSquadMember copy = new LineupSquadMember();
            copy.setLineup(target);
            copy.setPlayer(member.getPlayer());
            copy.setLineCode(member.getLineCode());
            copy.setCalled(member.isCalled());
            copy.setBenchOrder(member.getBenchOrder());
            copy.setReasonNotCalled(member.getReasonNotCalled());
            copy.setOverrideReason(member.getOverrideReason());
            memberRepository.save(copy);
        }
    }

    private void copySlots(LineupVersion source, LineupVersion target) {
        slotRepository.deleteByLineupId(target.getId());
        entityManager.flush();
        for (LineupSlot slot : slotRepository.findByLineupIdOrderBySortOrderAsc(source.getId())) {
            LineupSlot copy = new LineupSlot();
            copy.setLineup(target);
            copy.setPlayer(slot.getPlayer());
            copy.setSelection(slot.getSelection());
            copy.setPositionSlot(slot.getPositionSlot());
            copy.setSortOrder(slot.getSortOrder());
            copy.setCaptain(slot.isCaptain());
            copy.setOverrideReason(slot.getOverrideReason());
            copy.setCoordX(slot.getCoordX());
            copy.setCoordY(slot.getCoordY());
            slotRepository.save(copy);
        }
    }

    private Set<Long> calledIds(Long lineupId) {
        Set<Long> ids = new HashSet<>();
        memberRepository.findByLineupIdOrderByBenchOrderAsc(lineupId).stream().filter(LineupSquadMember::isCalled).forEach(member -> ids.add(member.getPlayer().getId()));
        return ids;
    }

    private List<Ranked> ranked(FootballMatch match) {
        Availability availability = availability();
        Map<Long, List<MatchRating>> forms = new HashMap<>();
        ratingRepository.findAll().forEach(rating -> forms.computeIfAbsent(rating.getPlayer().getId(), key -> new ArrayList<>()).add(rating));
        List<Ranked> rows = new ArrayList<>();
        for (Player player : playerRepository.findByActiveTrueOrderByRegisterNoAsc()) {
            List<BigDecimal> last = forms.getOrDefault(player.getId(), List.of()).stream()
                    .sorted(Comparator.comparing((MatchRating rating) -> rating.getMatch().getKickoff()).reversed())
                    .map(MatchRating::getFinalRating).filter(value -> value != null).limit(5).toList();
            double form = last.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            var marks = attendanceRepository.findByPlayerId(player.getId());
            long present = marks.stream().filter(mark -> mark.getStatus() == AttendanceMark.P || mark.getStatus() == AttendanceMark.L).count();
            double attendance = marks.isEmpty() ? 0 : (double) present / marks.size();
            String state = availability.of(player);
            rows.add(new Ranked(player, SquadRules.lineOf(player.getLineName(), player.getPosition()), form, attendance, "AVAILABLE".equals(state) || "DOUBTFUL".equals(state), state));
        }
        rows.sort(Comparator.comparing(Ranked::form).reversed().thenComparing(Comparator.comparing(Ranked::attendance).reversed())
                .thenComparing(row -> row.player().getJerseyNumber() == null ? 999 : row.player().getJerseyNumber()));
        return rows;
    }

    private Availability availability() {
        Set<Long> injured = new HashSet<>();
        Set<Long> doubtful = new HashSet<>();
        for (Injury injury : injuryRepository.findByStatusNot(InjuryStatus.CLEARED)) {
            if (injury.getStatus() == InjuryStatus.INJURED) {
                injured.add(injury.getPlayer().getId());
            } else {
                doubtful.add(injury.getPlayer().getId());
            }
        }
        ClubSettings settings = settingsService.current();
        Map<Long, Long> yellows = eventRepository.findByEventType(EventType.YELLOW_CARD).stream()
                .filter(event -> event.getPlayer() != null && event.getMatch().getSeason().getId().equals(settings.getSeason().getId()))
                .collect(java.util.stream.Collectors.groupingBy(event -> event.getPlayer().getId(), java.util.stream.Collectors.counting()));
        return new Availability(injured, doubtful, yellows, settings.getYellowSuspension());
    }

    private String reasonFor(String state) {
        return switch (state) {
            case "INJURED" -> "Injured";
            case "SUSPENDED" -> "Suspended";
            case "DOUBTFUL" -> "Not fit";
            default -> "Tactical";
        };
    }

    private Map<String, Integer> normalize(Map<String, Integer> plan) {
        Map<String, Integer> clean = new LinkedHashMap<>();
        for (String line : SquadRules.LINES) {
            clean.put(line, Math.max(0, plan.getOrDefault(line, 0)));
        }
        return clean;
    }

    private Map<String, Integer> readPlan(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return null;
        }
    }

    private String write(Map<String, Integer> plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (Exception ex) {
            throw BusinessException.badRequest("The squad plan could not be stored");
        }
    }

    private int ruleBench(FootballMatch match, ClubSettings settings) {
        return settings.getLineupMaxBench();
    }

    private LineupVersion latest(Long matchId) {
        return lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream().findFirst().orElse(null);
    }

    private void requireEditor() {
        if (SecurityUtils.hasRole("GOALKEEPER_COACH") && !SecurityUtils.hasRole("HEAD_COACH") && !SecurityUtils.hasRole("ASSISTANT_COACH") && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("The goalkeeper coach can only suggest a goalkeeper");
        }
        if (!SecurityUtils.isExecutive() && !SecurityUtils.hasRole("HEAD_COACH") && !SecurityUtils.hasRole("ASSISTANT_COACH")) {
            throw BusinessException.forbidden("You cannot edit this lineup");
        }
    }

    private boolean locked(LineupState status) {
        return status == LineupState.LOCKED || status == LineupState.FINISHED;
    }

    private boolean visible(LineupState status) {
        return status == LineupState.PUBLISHED || status == LineupState.LOCKED || status == LineupState.FINISHED;
    }

    private record Ranked(Player player, String line, double form, double attendance, boolean available, String state) {
    }

    private record Availability(Set<Long> injured, Set<Long> doubtful, Map<Long, Long> yellows, int yellowLimit) {
        String of(Player player) {
            if (player.getStatus() == PlayerStatus.INJURED || injured.contains(player.getId())) {
                return "INJURED";
            }
            if (player.getStatus() == PlayerStatus.SUSPENDED || yellows.getOrDefault(player.getId(), 0L) >= yellowLimit) {
                return "SUSPENDED";
            }
            if (player.getStatus() == PlayerStatus.LOANED || player.getStatus() == PlayerStatus.RELEASED || player.getStatus() == PlayerStatus.TRANSFERRED) {
                return "UNAVAILABLE";
            }
            if (doubtful.contains(player.getId())) {
                return "DOUBTFUL";
            }
            return "AVAILABLE";
        }
    }

    public record SizeWrite(Integer squadSize, Map<String, Integer> plan, Long version, Boolean useAsDefault, String overrideReason) {
    }

    public record SquadWrite(List<MemberWrite> players, Long version) {
    }

    public record MemberWrite(Long playerId, String line, boolean called, Integer benchOrder, String reasonNotCalled, String overrideReason) {
    }

    public record XiWrite(String formation, List<XiSlot> slots, Long version, String opponentFormation) {
    }

    public record XiSlot(Long playerId, String positionSlot, Integer x, Integer y, Boolean captain, String overrideReason) {
    }

    public record ConfirmWrite(String comment, String overrideReason, Instant meetingAt, String meetingPlace, Long version) {
    }

    public record DuplicateWrite(Long targetMatchId) {
    }
}
