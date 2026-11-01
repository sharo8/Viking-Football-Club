package rw.vikingfc.club.removal;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceRecordRepository;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.auth.OtpService;
import rw.vikingfc.club.bonus.BonusEarnedRepository;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.PlayerContract;
import rw.vikingfc.club.contract.PlayerContractRepository;
import rw.vikingfc.club.contract.TeamCaptainRepository;
import rw.vikingfc.club.finance.PayrollInstalmentRepository;
import rw.vikingfc.club.lineup.LineupSlot;
import rw.vikingfc.club.lineup.LineupSlotRepository;
import rw.vikingfc.club.lineup.LineupSquadMember;
import rw.vikingfc.club.lineup.LineupSquadMemberRepository;
import rw.vikingfc.club.lineup.LineupState;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.match.MatchPlayerRecord;
import rw.vikingfc.club.match.MatchPlayerRecordRepository;
import rw.vikingfc.club.media.FileStorageService;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerPhotoRepository;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.sanction.Fine;
import rw.vikingfc.club.sanction.FineRepository;
import rw.vikingfc.club.security.UserPrincipal;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.RolePolicy;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FormerPlayerService {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");
    private static final List<Role> EXECUTIVES = List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER);

    private final PlayerRepository players;
    private final PlayerContractRepository contracts;
    private final TeamCaptainRepository captains;
    private final LineupSlotRepository slots;
    private final LineupSquadMemberRepository squads;
    private final MatchPlayerRecordRepository records;
    private final FineRepository fines;
    private final BonusEarnedRepository bonuses;
    private final PayrollInstalmentRepository payroll;
    private final PlayerPhotoRepository photos;
    private final AttendanceRecordRepository attendance;
    private final FileStorageService files;
    private final UserRepository users;

    @PersistenceContext
    private EntityManager entityManager;
    private final AccountRemovalService accounts;
    private final PlayerDeletionRequestRepository requests;
    private final PlayerDeletionVoteRepository votes;
    private final OtpService otpService;
    private final NotificationService notifications;
    private final ClubMailService mail;
    private final AuditService auditService;
    private final Clock clock;

    public Map<String, Object> move(Long id, Map<String, Object> body) {
        assertMover();
        Player player = players.findByIdAndActiveTrue(id).orElseThrow(() -> NotFoundException.of("Player", id));
        if (player.getStatus() == PlayerStatus.FORMER) {
            throw BusinessException.badRequest("This player is already former", "ALREADY_FORMER");
        }
        String reason = text(body.get("exitReason"));
        RemovalRules.exitReason(reason);
        blockPublished(player.getId());
        player.setStatus(PlayerStatus.FORMER);
        player.setExitReason(reason.toUpperCase());
        player.setExitDate(date(body.get("exitDate")));
        player.setExitNote(text(body.get("note")));
        captains.findByPlayerIdAndEndedAtIsNull(player.getId()).forEach(row -> row.setEndedAt(Instant.now(clock)));
        contracts.findByPlayerIdAndActiveTrue(player.getId()).forEach(row -> row.setActive(false));
        slots.findByPlayerId(player.getId()).stream()
                .filter(slot -> slot.getLineup().getStatus() == LineupState.DRAFT)
                .forEach(slots::delete);
        squads.findByPlayerId(player.getId()).stream()
                .filter(member -> member.getLineup().getStatus() == LineupState.DRAFT)
                .forEach(squads::delete);
        auditService.record("MOVE_FORMER", "Player", id, player.getFullName(), reason, null);
        return view(player, null);
    }

    public Map<String, Object> restore(Long id, boolean reuseContract) {
        assertMover();
        Player player = players.findById(id).orElseThrow(() -> NotFoundException.of("Player", id));
        if (player.getStatus() != PlayerStatus.FORMER || Boolean.TRUE.equals(player.getAnonymized())) {
            throw BusinessException.badRequest("This player is not in former players", "NOT_FORMER");
        }
        player.setStatus(PlayerStatus.ACTIVE);
        player.setExitReason(null);
        player.setExitDate(null);
        if (reuseContract) {
            List<PlayerContract> history = contracts.findByPlayerIdOrderByStartDateDesc(player.getId());
            if (!history.isEmpty()) {
                history.get(0).setActive(true);
            }
        }
        auditService.record("RESTORE_FORMER", "Player", id, null, player.getFullName(), null);
        return view(player, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String q, String exitReason, String year, String history, String deletion) {
        String needle = q == null ? "" : q.toLowerCase();
        List<Map<String, Object>> rows = players.findByStatusAndActiveTrueOrderByFullNameAsc(PlayerStatus.FORMER).stream()
                .filter(player -> !Boolean.TRUE.equals(player.getAnonymized()))
                .filter(player -> needle.isBlank() || player.getFullName().toLowerCase().contains(needle))
                .filter(player -> exitReason == null || exitReason.isBlank() || exitReason.equalsIgnoreCase(player.getExitReason()))
                .filter(player -> year == null || year.isBlank() || (player.getExitDate() != null && String.valueOf(player.getExitDate().getYear()).equals(year)))
                .map(player -> view(player, open(player.getId())))
                .filter(row -> history == null || history.isBlank() || ("yes".equals(history) == Boolean.TRUE.equals(row.get("hasHistory"))))
                .filter(row -> deletion == null || deletion.isBlank() || deletion.equalsIgnoreCase(String.valueOf(row.get("deletionStatus"))))
                .toList();
        return Map.of("rows", rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> kpis() {
        List<Player> rows = players.findByStatusAndActiveTrueOrderByFullNameAsc(PlayerStatus.FORMER).stream()
                .filter(player -> !Boolean.TRUE.equals(player.getAnonymized())).toList();
        long withHistory = rows.stream().filter(player -> summary(player.getId()).hasHistory()).count();
        return Map.of(
                "total", rows.size(),
                "pending", requests.countByStatus("PENDING"),
                "withHistory", withHistory,
                "withoutHistory", rows.size() - withHistory
        );
    }

    public Map<String, Object> request(Long playerId, String reason, String confirmName) {
        UserPrincipal actor = voter();
        Player player = players.findById(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId));
        RemovalRules.formerOnly(player.getStatus() == PlayerStatus.FORMER && !Boolean.TRUE.equals(player.getAnonymized()));
        RemovalRules.executivesPresent(
                users.countByRoleAndActiveTrueAndDeletedAtIsNull(Role.PRESIDENT),
                users.countByRoleAndActiveTrueAndDeletedAtIsNull(Role.GENERAL_MANAGER),
                users.countByRoleAndActiveTrueAndDeletedAtIsNull(Role.FINANCE_MANAGER));
        RemovalRules.openRequest(requests.findFirstByPlayerIdAndStatus(playerId, "PENDING").isPresent());
        RemovalRules.deletionReason(reason);
        RemovalRules.confirmName(player.getFullName(), confirmName);
        History history = summary(playerId);
        PlayerDeletionRequest row = new PlayerDeletionRequest();
        row.setPlayerId(playerId);
        row.setReason(reason.trim());
        row.setRequestedBy(actor.getId());
        row.setStatus("PENDING");
        row.setHasHistory(history.hasHistory());
        row.setHistorySummary(history.json());
        requests.save(row);
        vote(row, actor, "APPROVE", "Requested the deletion");
        auditService.record("DELETION_REQUEST", "PlayerDeletionRequest", row.getId(), player.getFullName(), reason.trim(), null);
        tellAll(player, "Deletion requested", actor.getFullName() + " asked to delete " + player.getFullName() + ". " + history.json(), row.getId());
        return detail(row);
    }

    public Map<String, Object> approve(Long id, String comment, String otp) {
        return decide(id, "APPROVE", comment, otp);
    }

    public Map<String, Object> reject(Long id, String comment, String otp) {
        if (comment == null || comment.trim().length() < 3) {
            throw BusinessException.badRequest("A comment is required to reject", "COMMENT_REQUIRED");
        }
        return decide(id, "REJECT", comment, otp);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> queue(String status) {
        List<PlayerDeletionRequest> rows = status == null || status.isBlank()
                ? requests.findAll()
                : requests.findByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(this::detail).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> one(Long id) {
        return detail(requests.findById(id).orElseThrow(() -> NotFoundException.of("Request", id)));
    }

    private Map<String, Object> decide(Long id, String decision, String comment, String otp) {
        UserPrincipal actor = voter();
        stepUp(actor, otp);
        PlayerDeletionRequest row = requests.findById(id).orElseThrow(() -> NotFoundException.of("Request", id));
        if (!"PENDING".equals(row.getStatus())) {
            throw BusinessException.badRequest("This request is closed", "REQUEST_CLOSED");
        }
        RemovalRules.oneVote(votes.existsByRequestIdAndVoterRole(row.getId(), actor.getRole().name()));
        vote(row, actor, decision, comment);
        Player player = players.findById(row.getPlayerId()).orElse(null);
        String name = player == null ? "Player" : player.getFullName();
        if ("REJECT".equals(decision)) {
            row.setStatus("REJECTED");
            row.setResolvedAt(Instant.now(clock));
            auditService.record("DELETION_REJECTED", "PlayerDeletionRequest", row.getId(), name, comment, null);
            tellAll(player, "Deletion rejected", actor.getFullName() + " rejected the deletion of " + name + ". " + comment, row.getId());
            return detail(row);
        }
        int approvals = (int) votes.findByRequestIdOrderByDecidedAtAsc(row.getId()).stream().filter(item -> "APPROVE".equals(item.getDecision())).count();
        String next = RemovalRules.afterVotes(false, approvals);
        if ("APPROVED".equals(next)) {
            execute(row, player);
            auditService.record("DELETION_EXECUTED", "PlayerDeletionRequest", row.getId(), name, row.getReason(), null);
            tellAll(player, "Player deleted", name + " was permanently deleted.", row.getId());
        } else {
            auditService.record("DELETION_APPROVED", "PlayerDeletionRequest", row.getId(), name, approvals + " of 3", null);
            tellAll(player, approvals + " of 3 approved", actor.getFullName() + " approved the deletion of " + name + ".", row.getId());
        }
        return detail(row);
    }

    private void clearPersonalLinks(Long playerId) {
        for (String table : java.util.List.of("convocations", "lineup_flags", "match_ratings", "injuries", "team_captains")) {
            entityManager.createNativeQuery("DELETE FROM " + table + " WHERE player_id = ?1").setParameter(1, playerId).executeUpdate();
        }
        for (String table : java.util.List.of("match_events", "transfers", "bonus_rules", "content_posts", "media_assets")) {
            entityManager.createNativeQuery("UPDATE " + table + " SET player_id = NULL WHERE player_id = ?1").setParameter(1, playerId).executeUpdate();
        }
    }

    private void execute(PlayerDeletionRequest row, Player player) {
        row.setStatus("EXECUTED");
        row.setResolvedAt(Instant.now(clock));
        if (player == null) {
            return;
        }
        if (!row.isHasHistory()) {
            photos.findByPlayerIdOrderByCreatedAtDesc(player.getId()).forEach(photo -> {
                files.delete(photo.getUrl());
                photos.delete(photo);
            });
            files.delete(player.getPhotoUrl());
            contracts.findByPlayerIdOrderByStartDateDesc(player.getId()).forEach(contracts::delete);
            captains.findByPlayerIdAndEndedAtIsNull(player.getId()).forEach(captains::delete);
            attendance.findByPlayerId(player.getId()).forEach(attendance::delete);
            slots.findByPlayerId(player.getId()).forEach(slots::delete);
            squads.findByPlayerId(player.getId()).forEach(squads::delete);
            clearPersonalLinks(player.getId());
            if (player.getUserId() != null) {
                users.findById(player.getUserId()).ifPresent(user -> accounts.archiveLinked(user, row.getReason()));
            }
            players.delete(player);
            return;
        }
        files.delete(player.getPhotoUrl());
        player.setFullName("Deleted player #" + player.getId());
        player.setKnownAs(player.getFullName());
        player.setPhone(null);
        player.setEmail(null);
        player.setAddress(null);
        player.setDateOfBirth(null);
        player.setNationality(null);
        player.setPhotoUrl(null);
        player.setEmergencyContactName(null);
        player.setEmergencyContactPhone(null);
        player.setEmergencyContactEmail(null);
        player.setEmergencyContactRelationship(null);
        player.setNotes(null);
        player.setAnonymized(true);
        if (player.getUserId() != null) {
            users.findById(player.getUserId()).ifPresent(user -> accounts.archiveLinked(user, row.getReason()));
        }
    }

    private void vote(PlayerDeletionRequest row, UserPrincipal actor, String decision, String comment) {
        PlayerDeletionVote vote = new PlayerDeletionVote();
        vote.setRequestId(row.getId());
        vote.setVoterUserId(actor.getId());
        vote.setVoterRole(actor.getRole().name());
        vote.setDecision(decision);
        vote.setComment(comment == null ? "" : comment.trim());
        vote.setDecidedAt(Instant.now(clock));
        votes.save(vote);
    }

    private void stepUp(UserPrincipal actor, String otp) {
        if (!RolePolicy.otpRequired(actor.getRole().name())) {
            return;
        }
        if (otp == null || otp.isBlank()) {
            throw BusinessException.badRequest("Enter the code sent to your email", "OTP_REQUIRED");
        }
        otpService.verify(actor.getUsername(), OtpService.STEP_UP, otp);
    }

    private void blockPublished(Long playerId) {
        Instant now = Instant.now(clock);
        for (LineupSlot slot : slots.findByPlayerId(playerId)) {
            if (upcoming(slot.getLineup().getStatus(), slot.getLineup().getMatch().getKickoff(), now)) {
                throw BusinessException.badRequest("This player is in a published lineup for an upcoming match", "LINEUP_PUBLISHED");
            }
        }
        for (LineupSquadMember member : squads.findByPlayerId(playerId)) {
            if (upcoming(member.getLineup().getStatus(), member.getLineup().getMatch().getKickoff(), now)) {
                throw BusinessException.badRequest("This player is in a published lineup for an upcoming match", "LINEUP_PUBLISHED");
            }
        }
    }

    private boolean upcoming(LineupState status, Instant kickoff, Instant now) {
        return (status == LineupState.PUBLISHED || status == LineupState.LOCKED) && kickoff != null && kickoff.isAfter(now);
    }

    private History summary(Long playerId) {
        List<MatchPlayerRecord> played = records.findByPlayerId(playerId);
        int goals = played.stream().mapToInt(MatchPlayerRecord::getGoals).sum();
        long paid = payroll.findByPlayerIdOrderByDueDateAsc(playerId).stream().mapToLong(row -> row.getAmountPaid().longValue()).sum();
        List<Fine> fineRows = fines.findByPlayerIdOrderByFineDateAsc(playerId);
        long openFines = fineRows.stream().filter(fine -> fine.getStatus() != null && !"PAID".equals(fine.getStatus().name()) && !"WAIVED".equals(fine.getStatus().name())).count();
        int bonusCount = bonuses.findByPlayerIdOrderByEarnedOnDesc(playerId).size();
        boolean history = !played.isEmpty() || paid > 0 || !fineRows.isEmpty() || bonusCount > 0;
        return new History(played.size(), goals, paid, openFines, bonusCount, history);
    }

    private PlayerDeletionRequest open(Long playerId) {
        return requests.findFirstByPlayerIdAndStatus(playerId, "PENDING").orElse(null);
    }

    private Map<String, Object> view(Player player, PlayerDeletionRequest open) {
        History history = summary(player.getId());
        String deletion = "NONE";
        if (open != null) {
            int approvals = (int) votes.findByRequestIdOrderByDecidedAtAsc(open.getId()).stream().filter(vote -> "APPROVE".equals(vote.getDecision())).count();
            deletion = "PENDING_" + approvals;
        } else {
            deletion = requests.findByPlayerIdOrderByCreatedAtDesc(player.getId()).stream().findFirst()
                    .map(PlayerDeletionRequest::getStatus).filter(status -> "REJECTED".equals(status)).map(status -> "REJECTED").orElse("NONE");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", player.getId());
        row.put("fullName", player.getFullName());
        row.put("jersey", player.getJerseyNumber());
        row.put("position", player.getPosition().name());
        row.put("photoUrl", player.getPhotoUrl());
        row.put("exitReason", player.getExitReason());
        row.put("exitDate", player.getExitDate());
        row.put("exitNote", player.getExitNote());
        row.put("matches", history.matches());
        row.put("goals", history.goals());
        row.put("paid", history.paid());
        row.put("openFines", history.openFines());
        row.put("hasHistory", history.hasHistory());
        row.put("deletionStatus", deletion);
        row.put("requestId", open == null ? null : open.getId());
        return row;
    }

    private Map<String, Object> detail(PlayerDeletionRequest row) {
        Player player = players.findById(row.getPlayerId()).orElse(null);
        List<Map<String, Object>> voteRows = votes.findByRequestIdOrderByDecidedAtAsc(row.getId()).stream().map(vote -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", vote.getVoterRole());
            item.put("decision", vote.getDecision());
            item.put("comment", vote.getComment());
            item.put("at", vote.getDecidedAt());
            item.put("userId", vote.getVoterUserId());
            return item;
        }).toList();
        int approvals = (int) voteRows.stream().filter(item -> "APPROVE".equals(item.get("decision"))).count();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", row.getId());
        body.put("playerId", row.getPlayerId());
        body.put("player", player == null ? "Deleted player #" + row.getPlayerId() : player.getFullName());
        body.put("jersey", player == null ? null : player.getJerseyNumber());
        body.put("photoUrl", player == null ? null : player.getPhotoUrl());
        body.put("reason", row.getReason());
        body.put("status", row.getStatus());
        body.put("hasHistory", row.isHasHistory());
        body.put("history", row.getHistorySummary());
        body.put("requestedBy", row.getRequestedBy());
        body.put("approvals", approvals);
        body.put("votes", voteRows);
        body.put("createdAt", row.getCreatedAt());
        return body;
    }

    private void tellAll(Player player, String title, String sentence, Long requestId) {
        String when = ZonedDateTime.now(KIGALI).format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"));
        String link = "/admin/approvals?request=" + requestId;
        for (User user : users.findByRoleInAndActiveTrueAndDeletedAtIsNull(EXECUTIVES)) {
            String language = user.getPreferredLanguage() == null ? "en" : user.getPreferredLanguage();
            notifications.notify(user, "PLAYER_DELETION", title, sentence, link);
            mail.enqueue(user.getEmail(), title, mail.layout(title, "<p>" + sentence + "</p><p><a href=\"" + link + "\">Review and decide</a></p><p>" + when + " · Kigali</p>"),
                    "player-deletion", language, "PLAYER_DELETION", String.valueOf(requestId));
        }
    }

    private UserPrincipal voter() {
        UserPrincipal actor = SecurityUtils.principal();
        if (actor == null || !RolePolicy.executive(actor.getRole().name())) {
            throw BusinessException.forbidden("Only the President, General Manager or Finance Manager can do this");
        }
        return actor;
    }

    private void assertMover() {
        if (SecurityUtils.isExecutive() || SecurityUtils.hasRole("ASSISTANT_MANAGER")) {
            return;
        }
        throw BusinessException.forbidden("You cannot move this player");
    }

    private LocalDate date(Object value) {
        String raw = text(value);
        if (raw.isBlank()) {
            return LocalDate.now(clock);
        }
        return LocalDate.parse(raw.substring(0, Math.min(10, raw.length())));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record History(int matches, int goals, long paid, long openFines, int bonuses, boolean hasHistory) {
        String json() {
            return "{\"matches\":" + matches + ",\"goals\":" + goals + ",\"paid\":" + paid + ",\"fines\":" + openFines + ",\"bonuses\":" + bonuses + "}";
        }
    }
}
