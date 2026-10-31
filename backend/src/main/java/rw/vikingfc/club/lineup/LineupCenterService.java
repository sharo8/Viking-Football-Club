package rw.vikingfc.club.lineup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.bonus.BonusService;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.config.JpaConfig;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.match.EventType;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.match.MatchEvent;
import rw.vikingfc.club.match.MatchEventRepository;
import rw.vikingfc.club.match.MatchPlayerRecord;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.match.MatchService;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.medical.InjuryRepository;
import rw.vikingfc.club.medical.InjuryStatus;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.player.Position;
import rw.vikingfc.club.settings.ClubSettings;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.UserRepository;

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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LineupCenterService {

    private final MatchService matchService;
    private final LineupService lineupService;
    private final LineupVersionRepository lineupRepository;
    private final LineupSlotRepository slotRepository;
    private final PlayerRepository playerRepository;
    private final InjuryRepository injuryRepository;
    private final ConvocationRepository convocationRepository;
    private final MatchRatingRepository ratingRepository;
    private final LineupTransitionRepository transitionRepository;
    private final LineupFlagRepository flagRepository;
    private final LineupReminderRepository reminderRepository;
    private final MatchEventRepository eventRepository;
    private final FootballMatchRepository matchRepository;
    private final MatchPlayerRecordRepository recordRepository;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final ClubMailService mailService;
    private final AuditService auditService;
    private final BonusService bonusService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${app.mail.frontend-base-url:http://localhost:3000}")
    private String publicUrl;

    @Transactional(readOnly = true)
    public Map<String, Object> sheet(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = latest(matchId);
        if (ClubAccess.publishedOnly() && (lineup == null || !visible(lineup.getStatus()))) {
            Map<String, Object> hidden = new LinkedHashMap<>();
            hidden.put("match", matchView(match));
            hidden.put("lineup", null);
            hidden.put("slots", List.of());
            hidden.put("published", false);
            return hidden;
        }
        ClubSettings settings = settingsService.current();
        List<LineupSlot> slots = lineup == null ? List.of() : slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("match", matchView(match));
        body.put("lineup", lineup == null ? null : lineupView(lineup, match));
        body.put("slots", slots.stream().map(this::slotView).toList());
        body.put("formations", FormationBook.codes().stream().map(code -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", code);
            row.put("spots", FormationBook.spots(code));
            return row;
        }).toList());
        body.put("squad", squad(match, slots, settings));
        body.put("convocations", convocationRepository.findByMatchId(matchId).stream()
                .sorted(Comparator.comparing(call -> call.getPlayer().getJerseyNumber() == null ? 999 : call.getPlayer().getJerseyNumber()))
                .map(this::callView).toList());
        body.put("ratings", ratingsView(match, settings));
        body.put("events", matchService.events(matchId));
        body.put("history", history(matchId));
        body.put("flags", flagRepository.findByMatchIdOrderByCreatedAtDesc(matchId).stream().map(flag -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", flag.getPlayer().getId());
            row.put("note", flag.getNote());
            row.put("by", flag.getCreatedBy());
            return row;
        }).toList());
        body.put("permissions", permissions(lineup, match));
        body.put("settings", Map.of(
                "deadlineHours", settings.getLineupDeadlineHours(),
                "maxBench", settings.getLineupMaxBench(),
                "minSquad", settings.getLineupMinSquad(),
                "maxSquad", settings.getLineupMaxSquad(),
                "maxSubs", settings.getMaxSubstitutes(),
                "publicLineup", settings.isPublicLineup()));
        return body;
    }

    public Map<String, Object> save(Long matchId, LineupService.LineupWrite request) {
        requireEditor();
        LineupVersion lineup = latest(matchId);
        if (lineup != null && locked(lineup.getStatus()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.unprocessable("The lineup is locked", "LINEUP_LOCKED");
        }
        if (lineup != null && lineup.getEditingUntil() != null && lineup.getEditingUntil().isAfter(Instant.now(clock))
                && lineup.getEditingBy() != null && !lineup.getEditingBy().equals(SecurityUtils.email()) && !SecurityUtils.isExecutive()) {
            throw BusinessException.conflict("Editing now: " + lineup.getEditingBy(), "EDIT_CONFLICT", lineup.getEditingBy());
        }
        if (SecurityUtils.hasRole("GOALKEEPER_COACH") && !SecurityUtils.hasRole("HEAD_COACH") && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("The goalkeeper coach can only suggest a goalkeeper");
        }
        lineupService.save(matchId, request);
        return sheet(matchId);
    }

    public Map<String, Object> suggestGoalkeeper(Long matchId, Long playerId) {
        if (!SecurityUtils.hasRole("GOALKEEPER_COACH") && !SecurityUtils.hasRole("HEAD_COACH") && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only a coach can suggest the goalkeeper");
        }
        Player player = playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId));
        if (player.getPosition() != Position.GOALKEEPER) {
            throw BusinessException.unprocessable("The suggestion has to be a goalkeeper");
        }
        LineupVersion lineup = ensure(matchService.load(matchId));
        lineup.setGkSuggestionId(playerId);
        note(lineup, "suggest-gk", player.getKnownAs());
        return sheet(matchId);
    }

    public Map<String, Object> transition(Long matchId, String action, String comment) {
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = ensure(match);
        boolean executive = SecurityUtils.isExecutive();
        if ("submit".equals(action) && !executive && !SecurityUtils.hasRole("HEAD_COACH")) {
            throw BusinessException.forbidden("Only the head coach can submit the lineup");
        }
        if (("approve".equals(action) || "changes".equals(action) || "reject".equals(action) || "unlock".equals(action)) && !executive) {
            throw BusinessException.forbidden("Only the administration can review a lineup");
        }
        if ("publish".equals(action) && !executive && !SecurityUtils.hasRole("HEAD_COACH")) {
            throw BusinessException.forbidden("Only the head coach can publish an approved lineup");
        }
        if (("changes".equals(action) || "reject".equals(action) || "unlock".equals(action)) && (comment == null || comment.isBlank())) {
            throw BusinessException.badRequest("A comment is required", "COMMENT_REQUIRED");
        }
        if ("publish".equals(action) && lineup.getStatus() != LineupState.APPROVED && executive && (comment == null || comment.isBlank())) {
            throw BusinessException.badRequest("An override needs a written reason", "COMMENT_REQUIRED");
        }
        if ("submit".equals(action) || "publish".equals(action)) {
            strict(lineup);
            squadSize(matchId, lineup);
        }
        Instant now = Instant.now(clock);
        if ("submit".equals(action) && !executive && lineup.getDeadlineAt() != null && now.isAfter(lineup.getDeadlineAt())) {
            throw BusinessException.unprocessable("The 48-hour deadline has passed", "DEADLINE_PASSED");
        }
        if (("unlock".equals(action) || "save".equals(action)) && locked(lineup.getStatus()) && !executive) {
            throw BusinessException.unprocessable("The lineup is locked", "LINEUP_LOCKED");
        }
        LineupState next = LineupMachine.apply(lineup.getStatus(), action, executive);
        lineup.setStatus(next);
        lineup.setReviewComment(comment);
        if ("submit".equals(action)) {
            lineup.setSubmittedAt(now);
            lineup.setSubmittedBy(SecurityUtils.email());
            lineup.setSnapshotJson(snapshot(lineup));
            notificationService.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER),
                    "LINEUP", "Lineup submitted", match.getOpponent().getName(), "/admin/lineup?matchId=" + matchId);
        }
        if ("approve".equals(action)) {
            lineup.setApprovedAt(now);
            lineup.setApprovedBy(SecurityUtils.email());
            notifyCoach(match, "Lineup approved", "The administration approved the lineup.");
        }
        if ("changes".equals(action)) {
            notifyCoach(match, "Changes requested", comment);
        }
        if ("reject".equals(action)) {
            notifyCoach(match, "Lineup rejected", comment);
        }
        if ("publish".equals(action)) {
            lineup.setPublishedAt(now);
            lineup.setPublishedBy(SecurityUtils.email());
            if (lineup.getStatus() == LineupState.PUBLISHED && comment != null && lineup.getStatus() != LineupState.APPROVED) {
                lineup.setOverrideReason(comment);
            }
            if (match.getStatus() == MatchStatus.SCHEDULED) {
                match.setStatus(MatchStatus.LINEUP_PUBLISHED);
            }
            match.setFormation(lineup.getFormation());
            matchService.rebuildRecords(match);
            bonusService.sync(match);
            notificationService.notifyRoles(List.of(Role.SOCIAL_MEDIA_MANAGER, Role.MARKETING_MANAGER, Role.ASSISTANT_COACH,
                            Role.GOALKEEPER_COACH, Role.ASSISTANT_MANAGER, Role.DOCTOR, Role.HEAD_COACH),
                    "LINEUP", "Lineup published", match.getOpponent().getName(), "/admin/lineup?matchId=" + matchId);
        }
        if ("unlock".equals(action)) {
            lineup.setLockedAt(null);
            lineup.setOverrideReason(comment);
        }
        if ("publish".equals(action) && executive && comment != null) {
            lineup.setOverrideReason(comment);
        }
        note(lineup, action, comment);
        auditService.record(action.toUpperCase(), "Lineup", lineup.getId(), null, next.name(), comment);
        return sheet(matchId);
    }

    public Map<String, Object> saveCalls(Long matchId, CallWrite request) {
        if (!canLogistics() && !canCoach()) {
            throw BusinessException.forbidden("You cannot edit the call-up list");
        }
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = ensure(match);
        if (request.meetingAt() != null) lineup.setMeetingAt(request.meetingAt());
        if (request.meetingPlace() != null) lineup.setMeetingPlace(request.meetingPlace());
        if (request.kitNote() != null) lineup.setKitNote(request.kitNote());
        if (request.travelNote() != null) lineup.setTravelNote(request.travelNote());
        if (request.message() != null) lineup.setCallupMessage(request.message());
        if (request.players() != null && canCoach()) {
            for (CallRow row : request.players()) {
                Player player = playerRepository.findByIdAndActiveTrue(row.playerId()).orElseThrow(() -> NotFoundException.of("Player", row.playerId()));
                Convocation call = convocationRepository.findByMatchIdAndPlayerId(matchId, row.playerId()).orElseGet(Convocation::new);
                call.setMatch(match);
                call.setPlayer(player);
                call.setCalled(row.called());
                if (!row.called()) {
                    call.setReason(row.reason());
                    call.setReasonNote(row.reasonNote());
                    call.setResponse("PENDING");
                } else if (call.getResponse() == null) {
                    call.setResponse("PENDING");
                }
                convocationRepository.save(call);
            }
        }
        return sheet(matchId);
    }

    public Map<String, Object> sendCalls(Long matchId, boolean pendingOnly) {
        if (!canCoach() && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only a coach can send convocations");
        }
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = ensure(match);
        int sent = 0;
        int missing = 0;
        for (Convocation call : convocationRepository.findByMatchId(matchId)) {
            if (!call.isCalled()) continue;
            if (pendingOnly && call.getNotifiedAt() != null && "CONFIRMED".equals(call.getResponse())) continue;
            if (pendingOnly && !"PENDING".equals(call.getResponse())) continue;
            if (call.getPlayer().getEmail() == null || call.getPlayer().getEmail().isBlank()) {
                missing++;
                continue;
            }
            if (call.getToken() == null) {
                call.setToken(UUID.randomUUID().toString().replace("-", ""));
                call.setTokenExpires(match.getKickoff().plus(6, ChronoUnit.HOURS));
            }
            String link = publicUrl + "/convocation/" + call.getToken();
            String html = mailService.layout("Convocation", "<p>" + escape(match.getOpponent().getName()) + "</p><p>"
                    + escape(lineup.getMeetingPlace()) + "</p><p><a href=\"" + link + "\">Confirm or decline</a></p><p>"
                    + escape(lineup.getCallupMessage()) + "</p>");
            var row = mailService.enqueue(call.getPlayer().getEmail(), "Viking FC convocation", html, "convocation",
                    call.getPlayer().getPreferredLanguage(), "CONVOCATION", String.valueOf(call.getId()));
            mailService.dispatch(row.getId());
            call.setNotifiedAt(Instant.now(clock));
            userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(call.getPlayer().getEmail())
                    .ifPresent(user -> notificationService.notify(user, "LINEUP", "Match convocation", match.getOpponent().getName(), link));
            sent++;
        }
        note(lineup, pendingOnly ? "remind" : "send", sent + " sent");
        Map<String, Object> body = sheet(matchId);
        body.put("sent", sent);
        body.put("missingEmail", missing);
        return body;
    }

    public Map<String, Object> notifyOmitted(Long matchId) {
        if (!canCoach() && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only a coach can send convocations");
        }
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = ensure(match);
        int sent = 0;
        for (Convocation call : convocationRepository.findByMatchId(matchId)) {
            if (call.isCalled()) continue;
            if (call.getPlayer().getEmail() == null || call.getPlayer().getEmail().isBlank()) continue;
            String html = mailService.layout("Squad update", "<p>You are not in the matchday squad against "
                    + escape(match.getOpponent().getName()) + ".</p><p>" + escape(call.getReason()) + "</p><p>"
                    + escape(lineup.getCallupMessage()) + "</p>");
            var row = mailService.enqueue(call.getPlayer().getEmail(), "Viking FC squad update", html, "convocation",
                    call.getPlayer().getPreferredLanguage(), "CONVOCATION", String.valueOf(call.getId()));
            mailService.dispatch(row.getId());
            sent++;
        }
        note(lineup, "not-selected", sent + " sent");
        Map<String, Object> body = sheet(matchId);
        body.put("sent", sent);
        return body;
    }

    public Map<String, Object> publicCall(String token) {
        Convocation call = convocationRepository.findByToken(token).orElseThrow(() -> NotFoundException.of("Convocation", token));
        if (call.getTokenExpires() != null && call.getTokenExpires().isBefore(Instant.now(clock))) {
            throw BusinessException.unprocessable("This link has expired");
        }
        if (call.getReadAt() == null) {
            call.setReadAt(Instant.now(clock));
        }
        FootballMatch match = call.getMatch();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("player", call.getPlayer().getKnownAs());
        body.put("opponent", match.getOpponent().getName());
        body.put("kickoff", match.getKickoff());
        body.put("venue", match.getVenue());
        body.put("meetingAt", latest(match.getId()) == null ? null : latest(match.getId()).getMeetingAt());
        body.put("meetingPlace", latest(match.getId()) == null ? null : latest(match.getId()).getMeetingPlace());
        body.put("response", call.getResponse());
        return body;
    }

    public Map<String, Object> answerCall(String token, String response) {
        Convocation call = convocationRepository.findByToken(token).orElseThrow(() -> NotFoundException.of("Convocation", token));
        if (call.getAttempts() >= 8) {
            throw BusinessException.unprocessable("Too many attempts. Ask the club to resend the link.");
        }
        call.setAttempts(call.getAttempts() + 1);
        if (call.getTokenExpires() != null && call.getTokenExpires().isBefore(Instant.now(clock))) {
            throw BusinessException.unprocessable("This link has expired");
        }
        if (!"CONFIRMED".equals(response) && !"DECLINED".equals(response)) {
            throw BusinessException.badRequest("Answer with CONFIRMED or DECLINED");
        }
        call.setResponse(response);
        call.setConfirmedAt(Instant.now(clock));
        return publicCall(token);
    }

    public Map<String, Object> substitute(Long matchId, Long outId, Long inId, int minute, String reason) {
        if (!canCoach() && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only a coach can record a substitution");
        }
        FootballMatch match = matchService.load(matchId);
        if (minute < 0 || minute > settingsService.current().getMatchLengthMinutes() + match.getStoppageMinutes()) {
            throw BusinessException.unprocessable("That minute is outside the match");
        }
        long used = eventRepository.findByMatchIdOrderByEventMinuteAsc(matchId).stream()
                .filter(event -> event.getEventType() == EventType.SUB_IN).count();
        if (used >= settingsService.current().getMaxSubstitutes()) {
            throw BusinessException.unprocessable("The substitution limit for this match is reached");
        }
        boolean back = eventRepository.findByMatchIdOrderByEventMinuteAsc(matchId).stream()
                .anyMatch(event -> event.getEventType() == EventType.SUB_OUT && event.getPlayer() != null && event.getPlayer().getId().equals(inId));
        if (back) {
            throw BusinessException.unprocessable("A substituted player cannot come back on");
        }
        MatchEvent out = new MatchEvent();
        out.setMatch(match);
        out.setEventMinute(minute);
        out.setEventType(EventType.SUB_OUT);
        out.setPlayer(playerRepository.findByIdAndActiveTrue(outId).orElseThrow(() -> NotFoundException.of("Player", outId)));
        out.setNote(reason);
        MatchEvent in = new MatchEvent();
        in.setMatch(match);
        in.setEventMinute(minute);
        in.setEventType(EventType.SUB_IN);
        in.setPlayer(playerRepository.findByIdAndActiveTrue(inId).orElseThrow(() -> NotFoundException.of("Player", inId)));
        in.setRelatedPlayer(out.getPlayer());
        in.setNote(reason);
        eventRepository.save(out);
        eventRepository.save(in);
        if (match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.LINEUP_PUBLISHED) {
            match.setStatus(MatchStatus.LIVE);
        }
        LineupVersion lineup = latest(matchId);
        if (lineup != null && lineup.getStatus() == LineupState.PUBLISHED && !match.getKickoff().isAfter(Instant.now(clock))) {
            lineup.setStatus(LineupState.LOCKED);
            lineup.setLockedAt(Instant.now(clock));
        }
        matchService.rebuildRecords(match);
        if ("injury".equalsIgnoreCase(reason)) {
            notificationService.notifyRoles(List.of(Role.DOCTOR, Role.HEAD_COACH), "MEDICAL", "Injury substitution",
                    out.getPlayer().getKnownAs(), "/admin/lineup?matchId=" + matchId);
        }
        return sheet(matchId);
    }

    public Map<String, Object> rate(Long matchId, Long playerId, BigDecimal coach, String comment, Boolean motm) {
        if (!canRate()) {
            throw BusinessException.forbidden("You cannot rate players for this match");
        }
        if (coach != null && (coach.compareTo(BigDecimal.ONE) < 0 || coach.compareTo(BigDecimal.TEN) > 0)) {
            throw BusinessException.unprocessable("A coach rating is between 1.0 and 10.0");
        }
        FootballMatch match = matchService.load(matchId);
        LineupVersion lineup = latest(matchId);
        if (lineup != null && lineup.isRatingsClosed() && !SecurityUtils.isExecutive()) {
            throw BusinessException.unprocessable("Ratings are closed");
        }
        if (SecurityUtils.hasRole("GOALKEEPER_COACH") && !SecurityUtils.hasRole("HEAD_COACH") && !SecurityUtils.isExecutive()) {
            Player keeper = playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId));
            if (keeper.getPosition() != Position.GOALKEEPER) {
                throw BusinessException.forbidden("The goalkeeper coach rates goalkeepers");
            }
        }
        ClubSettings settings = settingsService.current();
        matchService.rebuildRecords(match);
        MatchPlayerRecord record = recordRepository.findByMatchIdAndPlayerId(matchId, playerId).orElse(null);
        int minutes = record == null ? 0 : record.getMinutesPlayed();
        if (minutes < 15) {
            throw BusinessException.unprocessable("A player with under 15 minutes is not rated");
        }
        MatchRating rating = ratingRepository.findByMatchIdAndPlayerId(matchId, playerId).orElseGet(MatchRating::new);
        rating.setMatch(match);
        rating.setPlayer(playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId)));
        rating.setMinutesPlayed(minutes);
        rating.setCoachRating(coach);
        rating.setComment(comment);
        rating.setRatedBy(SecurityUtils.email());
        if (Boolean.TRUE.equals(motm)) {
            ratingRepository.findByMatchId(matchId).forEach(other -> other.setMotm(false));
            rating.setMotm(true);
            eventRepository.findByMatchIdOrderByEventMinuteAsc(matchId).stream()
                    .filter(event -> event.getEventType() == EventType.MAN_OF_THE_MATCH)
                    .forEach(eventRepository::delete);
            MatchEvent event = new MatchEvent();
            event.setMatch(match);
            event.setEventMinute(settings.getMatchLengthMinutes());
            event.setEventType(EventType.MAN_OF_THE_MATCH);
            event.setPlayer(rating.getPlayer());
            eventRepository.save(event);
            bonusService.sync(match);
        }
        applyScore(rating, record, match, settings);
        ratingRepository.save(rating);
        if (rating.getFinalRating() != null && record != null) {
            record.setRating(rating.getFinalRating());
        }
        return sheet(matchId);
    }

    public Map<String, Object> closeRatings(Long matchId) {
        if (!SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only the administration can close ratings");
        }
        LineupVersion lineup = ensure(matchService.load(matchId));
        lineup.setRatingsClosed(true);
        note(lineup, "close-ratings", null);
        return sheet(matchId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> leaderboard() {
        Map<Long, List<MatchRating>> grouped = ratingRepository.findAll().stream()
                .filter(row -> row.getFinalRating() != null)
                .collect(Collectors.groupingBy(row -> row.getPlayer().getId()));
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.forEach((playerId, list) -> {
            double average = list.stream().mapToDouble(row -> row.getFinalRating().doubleValue()).average().orElse(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", playerId);
            row.put("name", list.get(0).getPlayer().getKnownAs());
            row.put("photoUrl", list.get(0).getPlayer().getPhotoUrl());
            row.put("position", list.get(0).getPlayer().getPosition().getLabel());
            row.put("matches", list.size());
            row.put("average", BigDecimal.valueOf(average).setScale(1, java.math.RoundingMode.HALF_UP));
            rows.add(row);
        });
        rows.sort(Comparator.comparing((Map<String, Object> row) -> (BigDecimal) row.get("average")).reversed());
        return rows;
    }

    public Map<String, Object> restore(Long matchId, int revision) {
        requireEditor();
        LineupVersion source = lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream()
                .filter(version -> version.getRevision() == revision).findFirst()
                .orElseThrow(() -> NotFoundException.of("Lineup version", revision));
        List<LineupService.SlotWrite> slots = new ArrayList<>();
        for (LineupSlot slot : slotRepository.findByLineupIdOrderBySortOrderAsc(source.getId())) {
            slots.add(new LineupService.SlotWrite(slot.getPlayer().getId(), slot.getSelection(), slot.getPositionSlot(),
                    slot.getSortOrder(), slot.isCaptain(), slot.getOverrideReason(), slot.getCoordX(), slot.getCoordY()));
        }
        LineupVersion current = latest(matchId);
        if (current != null && current.getId().equals(source.getId())) {
            return sheet(matchId);
        }
        if (current != null && current.getStatus() != LineupState.DRAFT && current.getStatus() != LineupState.CHANGES_REQUESTED) {
            current.setStatus(LineupState.DRAFT);
        }
        lineupService.save(matchId, new LineupService.LineupWrite(source.getFormation(), slots, null, source.getOpponentFormation(), source.getOpponentNames()));
        note(latest(matchId), "restore", "v" + revision);
        return sheet(matchId);
    }

    public Map<String, Object> flag(Long matchId, Long playerId, String noteText) {
        if (!SecurityUtils.hasRole("DOCTOR") && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only the doctor can flag a medical concern");
        }
        LineupFlag flag = new LineupFlag();
        flag.setMatch(matchService.load(matchId));
        flag.setPlayer(playerRepository.findByIdAndActiveTrue(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId)));
        flag.setNote(noteText == null ? "" : noteText);
        flagRepository.save(flag);
        notificationService.notifyRoles(List.of(Role.HEAD_COACH, Role.PRESIDENT), "MEDICAL", "Medical flag", flag.getNote(), "/admin/lineup?matchId=" + matchId);
        return sheet(matchId);
    }

    public void sweepDeadlines() {
        ClubSettings settings = settingsService.current();
        Instant now = Instant.now(clock);
        for (FootballMatch match : matchRepository.findBySeasonIdOrderByKickoffAsc(settings.getSeason().getId())) {
            if (match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.POSTPONED) continue;
            LineupVersion lineup = latest(match.getId());
            boolean submitted = lineup != null && lineup.getSubmittedAt() != null;
            String kind = LineupMachine.reminder(match.getKickoff(), now, submitted, settings.getLineupDeadlineHours());
            if (kind != null && !reminderRepository.existsByMatchIdAndKind(match.getId(), kind)) {
                try {
                    LineupReminder reminder = new LineupReminder();
                    reminder.setMatch(match);
                    reminder.setKind(kind);
                    reminderRepository.saveAndFlush(reminder);
                    if ("OVERDUE".equals(kind)) {
                        notificationService.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER),
                                "LINEUP", "Lineup overdue", "Build the lineup for " + match.getOpponent().getName(), "/admin/lineup?matchId=" + match.getId());
                    } else {
                        notifyCoach(match, "Lineup deadline", "Submit the lineup before kick-off minus " + settings.getLineupDeadlineHours() + " hours.");
                    }
                } catch (DataIntegrityViolationException ignored) {
                    // Another run already recorded this reminder.
                }
            }
            if (lineup != null && lineup.getStatus() == LineupState.PUBLISHED && !match.getKickoff().isAfter(now)) {
                lineup.setStatus(LineupState.LOCKED);
                lineup.setLockedAt(now);
            }
            if (lineup != null && match.getStatus() == MatchStatus.FINISHED && lineup.getStatus() != LineupState.FINISHED) {
                if (lineup.getStatus() == LineupState.PUBLISHED || lineup.getStatus() == LineupState.LOCKED) {
                    lineup.setStatus(LineupState.FINISHED);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> publicNext() {
        ClubSettings settings = settingsService.current();
        if (!settings.isPublicLineup()) {
            return Map.of("published", false);
        }
        for (FootballMatch match : matchRepository.findBySeasonIdOrderByKickoffAsc(settings.getSeason().getId())) {
            LineupVersion lineup = latest(match.getId());
            if (lineup != null && visible(lineup.getStatus())) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("published", true);
                body.put("match", matchView(match));
                body.put("formation", lineup.getFormation());
                body.put("slots", slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId()).stream()
                        .filter(slot -> slot.getSelection() == Selection.STARTING).map(this::slotView).toList());
                return body;
            }
        }
        return Map.of("published", false);
    }

    private void applyScore(MatchRating rating, MatchPlayerRecord record, FootballMatch match, ClubSettings settings) {
        boolean defensive = rating.getPlayer().getPosition() == Position.GOALKEEPER
                || "Defenders".equals(rating.getPlayer().getLineName());
        BigDecimal system = RatingMath.system(rating.getMinutesPlayed(),
                record == null ? 0 : record.getGoals(), record == null ? 0 : record.getAssists(),
                record == null ? 0 : record.getYellowCards(), record == null ? 0 : record.getRedCards(),
                match.getGoalsAgainst() == 0 && (match.getStatus() == MatchStatus.FINISHED || match.getStatus() == MatchStatus.LIVE),
                defensive, rating.isMotm(), settings.getRatingBase(), settings.getRatingGoal(), settings.getRatingAssist(),
                settings.getRatingClean(), settings.getRatingMotm(), settings.getRatingYellow(), settings.getRatingRed());
        rating.setSystemScore(system);
        rating.setFinalRating(RatingMath.blend(rating.getCoachRating(), system, settings.getRatingCoachPct()));
    }

    private List<Map<String, Object>> ratingsView(FootballMatch match, ClubSettings settings) {
        Map<Long, MatchPlayerRecord> records = recordRepository.findByMatchId(match.getId()).stream()
                .collect(Collectors.toMap(row -> row.getPlayer().getId(), row -> row, (a, b) -> a));
        return ratingRepository.findByMatchId(match.getId()).stream().map(rating -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerId", rating.getPlayer().getId());
            row.put("name", rating.getPlayer().getKnownAs());
            row.put("photoUrl", rating.getPlayer().getPhotoUrl());
            row.put("position", rating.getPlayer().getPosition().getLabel());
            row.put("minutes", rating.getMinutesPlayed());
            row.put("coach", rating.getCoachRating());
            row.put("system", rating.getSystemScore());
            row.put("final", rating.getFinalRating());
            row.put("auto", rating.getCoachRating() == null);
            row.put("motm", rating.isMotm());
            row.put("comment", rating.getComment());
            MatchPlayerRecord record = records.get(rating.getPlayer().getId());
            row.put("goals", record == null ? 0 : record.getGoals());
            row.put("assists", record == null ? 0 : record.getAssists());
            row.put("yellow", record == null ? 0 : record.getYellowCards());
            row.put("red", record == null ? 0 : record.getRedCards());
            List<BigDecimal> last = ratingRepository.findByPlayerId(rating.getPlayer().getId()).stream()
                    .sorted(Comparator.comparing((MatchRating rated) -> rated.getMatch().getKickoff()).reversed())
                    .map(MatchRating::getFinalRating).filter(value -> value != null).limit(5).toList();
            row.put("last", last);
            return row;
        }).toList();
    }

    private List<Map<String, Object>> squad(FootballMatch match, List<LineupSlot> slots, ClubSettings settings) {
        Set<Long> injured = injuryRepository.findByStatusNot(InjuryStatus.CLEARED).stream()
                .map(injury -> injury.getPlayer().getId()).collect(Collectors.toSet());
        Map<Long, Long> yellows = eventRepository.findByEventType(EventType.YELLOW_CARD).stream()
                .filter(event -> event.getPlayer() != null && event.getMatch().getSeason().getId().equals(match.getSeason().getId()))
                .collect(Collectors.groupingBy(event -> event.getPlayer().getId(), Collectors.counting()));
        Map<Long, LineupSlot> used = slots.stream().collect(Collectors.toMap(slot -> slot.getPlayer().getId(), slot -> slot, (a, b) -> a));
        Map<Long, List<MatchRating>> forms = ratingRepository.findAll().stream().collect(Collectors.groupingBy(row -> row.getPlayer().getId()));
        return playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                .sorted(Comparator.comparing(player -> player.getJerseyNumber() == null ? 999 : player.getJerseyNumber()))
                .map(player -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", player.getId());
                    row.put("fullName", player.getFullName());
                    row.put("knownAs", player.getKnownAs());
                    row.put("jersey", player.getJerseyNumber());
                    row.put("position", player.getPosition().name());
                    row.put("positionLabel", player.getPosition().getLabel());
                    row.put("line", player.getLineName());
                    row.put("photoUrl", player.getPhotoUrl());
                    row.put("email", player.getEmail() != null && !player.getEmail().isBlank());
                    row.put("age", player.getDateOfBirth() == null ? null : Period.between(player.getDateOfBirth(), LocalDate.now(JpaConfig.KIGALI)).getYears());
                    String availability = "AVAILABLE";
                    if (player.getStatus() == PlayerStatus.INJURED || injured.contains(player.getId())) availability = "INJURED";
                    else if (player.getStatus() == PlayerStatus.SUSPENDED || yellows.getOrDefault(player.getId(), 0L) >= settings.getYellowSuspension()) availability = "SUSPENDED";
                    else if (player.getStatus() == PlayerStatus.LOANED || player.getStatus() == PlayerStatus.RELEASED || player.getStatus() == PlayerStatus.TRANSFERRED) availability = "UNAVAILABLE";
                    row.put("availability", availability);
                    LineupSlot slot = used.get(player.getId());
                    row.put("used", slot == null ? null : slot.getSelection().name());
                    List<BigDecimal> last = forms.getOrDefault(player.getId(), List.of()).stream()
                            .sorted(Comparator.comparing((MatchRating rating) -> rating.getMatch().getKickoff()).reversed())
                            .map(MatchRating::getFinalRating).filter(value -> value != null).limit(5).toList();
                    row.put("form", last.isEmpty() ? null : BigDecimal.valueOf(last.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0)).setScale(1, java.math.RoundingMode.HALF_UP));
                    return row;
                }).toList();
    }

    private void strict(LineupVersion lineup) {
        List<LineupSlot> slots = slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId());
        Set<Long> unavailable = injuryRepository.findByStatusNot(InjuryStatus.CLEARED).stream().map(injury -> injury.getPlayer().getId()).collect(Collectors.toSet());
        LineupValidator.validate(slots.stream()
                .map(slot -> new LineupValidator.SlotInput(slot.getPlayer().getId(), slot.getSelection(), slot.getPositionSlot(), slot.getOverrideReason()))
                .toList(), slots.stream().map(LineupSlot::getPlayer).toList(), settingsService.current().getLineupMaxBench(), unavailable);
        boolean gkSlot = slots.stream().filter(slot -> slot.getSelection() == Selection.STARTING && "GK".equals(slot.getPositionSlot()))
                .anyMatch(slot -> slot.getPlayer().getPosition() != Position.GOALKEEPER);
        if (gkSlot) {
            throw BusinessException.unprocessable("The goalkeeper slot has to be a goalkeeper", "LINEUP_INCOMPLETE");
        }
    }

    private void squadSize(Long matchId, LineupVersion lineup) {
        ClubSettings settings = settingsService.current();
        long called = convocationRepository.findByMatchId(matchId).stream().filter(Convocation::isCalled).count();
        if (called == 0) {
            called = slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId()).size();
        }
        if (called < settings.getLineupMinSquad() || called > settings.getLineupMaxSquad()) {
            throw BusinessException.unprocessable("The called-up squad must be between " + settings.getLineupMinSquad() + " and " + settings.getLineupMaxSquad(), "LINEUP_INCOMPLETE");
        }
    }

    private List<Map<String, Object>> history(Long matchId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LineupVersion version : lineupRepository.findByMatchIdOrderByRevisionDesc(matchId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("revision", version.getRevision());
            row.put("status", version.getStatus());
            row.put("formation", version.getFormation());
            row.put("at", version.getUpdatedAt());
            row.put("by", version.getUpdatedBy());
            row.put("comment", version.getReviewComment());
            row.put("players", slotRepository.findByLineupIdOrderBySortOrderAsc(version.getId()).stream().map(slot -> {
                Map<String, Object> player = new LinkedHashMap<>();
                player.put("playerId", slot.getPlayer().getId());
                player.put("name", slot.getPlayer().getKnownAs());
                player.put("slot", slot.getPositionSlot());
                player.put("selection", slot.getSelection().name());
                return player;
            }).toList());
            rows.add(row);
        }
        for (LineupTransition transition : transitionRepository.findByMatchIdOrderByCreatedAtDesc(matchId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("action", transition.getAction());
            row.put("revision", transition.getRevision());
            row.put("at", transition.getCreatedAt());
            row.put("by", transition.getActor());
            row.put("comment", transition.getComment());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> permissions(LineupVersion lineup, FootballMatch match) {
        LineupState status = lineup == null ? LineupState.DRAFT : lineup.getStatus();
        boolean executive = SecurityUtils.isExecutive();
        boolean head = executive || SecurityUtils.hasRole("HEAD_COACH");
        boolean coach = head || SecurityUtils.hasRole("ASSISTANT_COACH");
        boolean open = status == LineupState.DRAFT || status == LineupState.CHANGES_REQUESTED;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("edit", coach && (open || (executive && !locked(status))));
        body.put("submit", head && (status == LineupState.DRAFT || status == LineupState.CHANGES_REQUESTED));
        body.put("approve", executive && status == LineupState.SUBMITTED);
        body.put("publish", (head && status == LineupState.APPROVED) || (executive && !locked(status) && status != LineupState.FINISHED));
        body.put("unlock", executive && status == LineupState.LOCKED);
        body.put("rate", canRate() && (match.getStatus() == MatchStatus.LIVE || match.getStatus() == MatchStatus.FINISHED || status == LineupState.LOCKED || status == LineupState.FINISHED));
        body.put("subs", coach && !match.getKickoff().isAfter(Instant.now(clock)));
        body.put("calls", coach || canLogistics());
        body.put("graphic", visible(status) && (executive || head || ClubAccess.publishedOnly() || SecurityUtils.hasRole("SOCIAL_MEDIA_MANAGER") || SecurityUtils.hasRole("MARKETING_MANAGER")));
        body.put("medical", SecurityUtils.hasRole("DOCTOR") || executive);
        body.put("suggestGk", SecurityUtils.hasRole("GOALKEEPER_COACH") || head);
        return body;
    }

    private Map<String, Object> matchView(FootballMatch match) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", match.getId());
        body.put("opponent", match.getOpponent().getName());
        body.put("opponentShort", match.getOpponent().getShortName());
        body.put("opponentLogo", match.getOpponent().getLogoUrl());
        body.put("kickoff", match.getKickoff());
        body.put("venue", match.getVenue());
        body.put("home", match.isHomeMatch());
        body.put("competition", match.getCompetition());
        body.put("referee", match.getReferee());
        body.put("status", match.getStatus());
        body.put("goalsFor", match.getGoalsFor());
        body.put("goalsAgainst", match.getGoalsAgainst());
        return body;
    }

    private Map<String, Object> lineupView(LineupVersion lineup, FootballMatch match) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", lineup.getId());
        body.put("status", lineup.getStatus());
        body.put("formation", lineup.getFormation() == null ? "4-3-3" : lineup.getFormation());
        body.put("version", lineup.getVersion());
        body.put("revision", lineup.getRevision());
        body.put("deadlineAt", lineup.getDeadlineAt() == null ? match.getKickoff().minusSeconds(settingsService.current().getLineupDeadlineHours() * 3600L) : lineup.getDeadlineAt());
        body.put("submittedAt", lineup.getSubmittedAt());
        body.put("approvedAt", lineup.getApprovedAt());
        body.put("publishedAt", lineup.getPublishedAt());
        body.put("reviewComment", lineup.getReviewComment());
        body.put("editingBy", lineup.getEditingUntil() != null && lineup.getEditingUntil().isAfter(Instant.now(clock)) ? lineup.getEditingBy() : null);
        body.put("opponentFormation", lineup.getOpponentFormation());
        body.put("opponentNames", lineup.getOpponentNames());
        body.put("meetingAt", lineup.getMeetingAt());
        body.put("meetingPlace", lineup.getMeetingPlace());
        body.put("kitNote", lineup.getKitNote());
        body.put("travelNote", lineup.getTravelNote());
        body.put("message", lineup.getCallupMessage());
        body.put("ratingsClosed", lineup.isRatingsClosed());
        body.put("gkSuggestionId", lineup.getGkSuggestionId());
        return body;
    }

    private Map<String, Object> slotView(LineupSlot slot) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerId", slot.getPlayer().getId());
        body.put("name", slot.getPlayer().getKnownAs());
        body.put("fullName", slot.getPlayer().getFullName());
        body.put("jersey", slot.getPlayer().getJerseyNumber());
        body.put("photoUrl", slot.getPlayer().getPhotoUrl());
        body.put("position", slot.getPlayer().getPosition().getLabel());
        body.put("line", slot.getPlayer().getLineName());
        body.put("selection", slot.getSelection());
        body.put("positionSlot", slot.getPositionSlot());
        body.put("captain", slot.isCaptain());
        body.put("x", slot.getCoordX());
        body.put("y", slot.getCoordY());
        body.put("overrideReason", slot.getOverrideReason());
        return body;
    }

    private Map<String, Object> callView(Convocation call) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerId", call.getPlayer().getId());
        body.put("called", call.isCalled());
        body.put("response", call.getResponse());
        body.put("reason", call.getReason());
        body.put("reasonNote", call.getReasonNote());
        body.put("notifiedAt", call.getNotifiedAt());
        body.put("readAt", call.getReadAt());
        body.put("confirmedAt", call.getConfirmedAt());
        body.put("hasEmail", call.getPlayer().getEmail() != null && !call.getPlayer().getEmail().isBlank());
        return body;
    }

    private LineupVersion latest(Long matchId) {
        return lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream().findFirst().orElse(null);
    }

    private LineupVersion ensure(FootballMatch match) {
        LineupVersion lineup = latest(match.getId());
        if (lineup != null) {
            if (lineup.getDeadlineAt() == null) {
                lineup.setDeadlineAt(match.getKickoff().minusSeconds(settingsService.current().getLineupDeadlineHours() * 3600L));
            }
            return lineup;
        }
        LineupVersion created = new LineupVersion();
        created.setMatch(match);
        created.setRevision(1);
        created.setStatus(LineupState.DRAFT);
        created.setFormation("4-3-3");
        created.setDeadlineAt(match.getKickoff().minusSeconds(settingsService.current().getLineupDeadlineHours() * 3600L));
        return lineupRepository.save(created);
    }

    private void note(LineupVersion lineup, String action, String comment) {
        if (lineup == null) return;
        LineupTransition transition = new LineupTransition();
        transition.setMatch(lineup.getMatch());
        transition.setRevision(lineup.getRevision());
        transition.setAction(action);
        transition.setActor(SecurityUtils.email());
        transition.setComment(comment);
        transitionRepository.save(transition);
    }

    private String snapshot(LineupVersion lineup) {
        ArrayNode array = objectMapper.createArrayNode();
        for (LineupSlot slot : slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId())) {
            ObjectNode node = array.addObject();
            node.put("playerId", slot.getPlayer().getId());
            node.put("slot", slot.getPositionSlot());
            node.put("selection", slot.getSelection().name());
        }
        String json = array.toString();
        return json.length() > 4000 ? json.substring(0, 4000) : json;
    }

    private void notifyCoach(FootballMatch match, String title, String body) {
        userRepository.findByRoleInAndActiveTrueAndDeletedAtIsNull(List.of(Role.HEAD_COACH)).forEach(user -> {
            notificationService.notify(user, "LINEUP", title, body, "/admin/lineup?matchId=" + match.getId());
            if (user.getEmail() != null) {
                var mail = mailService.enqueue(user.getEmail(), title, mailService.layout(title, "<p>" + escape(body) + "</p>"),
                        "lineup", "en", "LINEUP", String.valueOf(match.getId()));
                mailService.dispatch(mail.getId());
            }
        });
    }

    private void requireEditor() {
        if (!canCoach() && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("You cannot edit this lineup");
        }
    }

    private boolean canCoach() {
        return SecurityUtils.isExecutive() || SecurityUtils.hasRole("HEAD_COACH") || SecurityUtils.hasRole("ASSISTANT_COACH");
    }

    private boolean canLogistics() {
        return SecurityUtils.hasRole("ASSISTANT_MANAGER") || SecurityUtils.isExecutive();
    }

    private boolean canRate() {
        return SecurityUtils.isExecutive() || SecurityUtils.hasRole("HEAD_COACH") || SecurityUtils.hasRole("ASSISTANT_COACH") || SecurityUtils.hasRole("GOALKEEPER_COACH");
    }

    private boolean locked(LineupState status) {
        return status == LineupState.LOCKED || status == LineupState.FINISHED;
    }

    private boolean visible(LineupState status) {
        return status == LineupState.PUBLISHED || status == LineupState.LOCKED || status == LineupState.FINISHED;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public record CallWrite(List<CallRow> players, Instant meetingAt, String meetingPlace, String kitNote, String travelNote, String message) {
    }

    public record CallRow(Long playerId, boolean called, String reason, String reasonNote) {
    }

}
