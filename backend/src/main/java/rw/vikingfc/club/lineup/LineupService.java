package rw.vikingfc.club.lineup;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.bonus.BonusService;
import rw.vikingfc.club.common.access.ClubAccess;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.contract.CaptainRank;
import rw.vikingfc.club.contract.TeamCaptainRepository;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.MatchService;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.medical.InjuryRepository;
import rw.vikingfc.club.medical.InjuryStatus;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.settings.SettingsService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.user.Role;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LineupService {

    private final MatchService matchService;
    private final LineupVersionRepository lineupRepository;
    private final LineupSlotRepository slotRepository;
    private final PlayerRepository playerRepository;
    private final InjuryRepository injuryRepository;
    private final TeamCaptainRepository captainRepository;
    private final SettingsService settingsService;
    private final BonusService bonusService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EntityManager entityManager;
    private final Clock clock;

    public Map<String, Object> save(Long matchId, LineupWrite request) {
        FootballMatch match = matchService.load(matchId);
        guardEditable(match);
        List<SlotWrite> incoming = request.slots() == null ? List.of() : request.slots();
        List<Player> players = incoming.stream()
                .map(slot -> playerRepository.findByIdAndActiveTrue(slot.playerId())
                        .orElseThrow(() -> rw.vikingfc.club.common.exception.NotFoundException.of("Player", slot.playerId())))
                .toList();
        Set<Long> unavailable = injuryRepository.findByStatusNot(InjuryStatus.CLEARED).stream()
                .map(injury -> injury.getPlayer().getId()).collect(Collectors.toSet());
        List<LineupValidator.SlotInput> inputs = incoming.stream()
                .map(slot -> new LineupValidator.SlotInput(slot.playerId(), slot.selection(), slot.positionSlot(), slot.overrideReason()))
                .toList();
        LineupValidator.validateDraft(inputs, players, unavailable);
        LineupVersion latest = lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream().findFirst().orElse(null);
        boolean open = latest != null && (latest.getStatus() == LineupState.DRAFT || latest.getStatus() == LineupState.CHANGES_REQUESTED
                || (SecurityUtils.isExecutive() && (latest.getStatus() == LineupState.SUBMITTED || latest.getStatus() == LineupState.APPROVED)));
        LineupVersion lineup = open ? latest : newVersion(match);
        if (lineup.getId() != null && request.version() != null && !request.version().equals(lineup.getVersion())) {
            throw BusinessException.conflict("Someone else saved this lineup. Reload it and try again.", "EDIT_CONFLICT", null);
        }
        lineup.setFormation(request.formation());
        lineupRepository.save(lineup);
        entityManager.flush();
        slotRepository.deleteByLineupId(lineup.getId());
        entityManager.flush();
        int order = 1;
        for (SlotWrite slot : incoming) {
            LineupSlot row = new LineupSlot();
            row.setLineup(lineup);
            row.setPlayer(players.stream().filter(player -> player.getId().equals(slot.playerId())).findFirst().orElseThrow());
            row.setSelection(slot.selection());
            row.setPositionSlot(slot.positionSlot());
            row.setSortOrder(slot.sortOrder() == null ? order : slot.sortOrder());
            row.setCaptain(Boolean.TRUE.equals(slot.captain()));
            row.setOverrideReason(slot.overrideReason());
            row.setCoordX(slot.x());
            row.setCoordY(slot.y());
            slotRepository.save(row);
            order++;
        }
        match.setFormation(request.formation());
        if (lineup.getDeadlineAt() == null) {
            lineup.setDeadlineAt(match.getKickoff().minusSeconds(settingsService.current().getLineupDeadlineHours() * 3600L));
        }
        if (request.opponentFormation() != null) lineup.setOpponentFormation(request.opponentFormation());
        if (request.opponentNames() != null) lineup.setOpponentNames(request.opponentNames());
        lineup.setEditingBy(SecurityUtils.email());
        lineup.setEditingUntil(Instant.now(clock).plusSeconds(90));
        auditService.record("UPDATE", "Lineup", lineup.getId(), null, request.formation(), null);
        return present(match, lineup, false);
    }

    public Map<String, Object> publish(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        guardEditable(match);
        LineupVersion lineup = lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream().findFirst()
                .orElseThrow(() -> BusinessException.unprocessable("Build a lineup before publishing it"));
        List<LineupSlot> held = slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId());
        Set<Long> unavailable = injuryRepository.findByStatusNot(InjuryStatus.CLEARED).stream()
                .map(injury -> injury.getPlayer().getId()).collect(Collectors.toSet());
        LineupValidator.validate(held.stream()
                .map(slot -> new LineupValidator.SlotInput(slot.getPlayer().getId(), slot.getSelection(), slot.getPositionSlot(), slot.getOverrideReason()))
                .toList(), held.stream().map(LineupSlot::getPlayer).toList(), settingsService.current().getMaxSubstitutes(), unavailable);
        if (lineup.getStatus() == LineupState.PUBLISHED) {
            LineupVersion copy = newVersion(match);
            copy.setFormation(lineup.getFormation());
            lineupRepository.save(copy);
            for (LineupSlot slot : slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId())) {
                LineupSlot clone = new LineupSlot();
                clone.setLineup(copy);
                clone.setPlayer(slot.getPlayer());
                clone.setSelection(slot.getSelection());
                clone.setPositionSlot(slot.getPositionSlot());
                clone.setSortOrder(slot.getSortOrder());
                clone.setCaptain(slot.isCaptain());
                clone.setOverrideReason(slot.getOverrideReason());
                slotRepository.save(clone);
            }
            lineup = copy;
        }
        lineup.setStatus(LineupState.PUBLISHED);
        lineup.setPublishedAt(Instant.now(clock));
        lineup.setPublishedBy(SecurityUtils.email());
        match.setStatus(match.getStatus() == MatchStatus.LIVE || match.getStatus() == MatchStatus.FINISHED
                ? match.getStatus() : MatchStatus.LINEUP_PUBLISHED);
        match.setFormation(lineup.getFormation());
        matchService.rebuildRecords(match);
        bonusService.sync(match);
        notificationService.notifyRoles(List.of(Role.SOCIAL_MEDIA_MANAGER, Role.MARKETING_MANAGER, Role.ASSISTANT_COACH,
                        Role.GOALKEEPER_COACH, Role.ASSISTANT_MANAGER, Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER),
                "LINEUP", "Lineup published", match.getOpponent().getName(), "/matches/" + match.getId());
        auditService.record("PUBLISH", "Lineup", lineup.getId(), null, lineup.getFormation(), null);
        return present(match, lineup, false);
    }

    public Map<String, Object> unpublish(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        if (match.getKickoff().isBefore(Instant.now(clock)) && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can change a lineup after kickoff");
        }
        LineupVersion lineup = lineupRepository.findFirstByMatchIdAndStatusOrderByRevisionDesc(matchId, LineupState.PUBLISHED)
                .orElseThrow(() -> BusinessException.unprocessable("There is no published lineup"));
        lineup.setStatus(LineupState.DRAFT);
        lineup.setPublishedAt(null);
        if (match.getStatus() == MatchStatus.LINEUP_PUBLISHED) {
            match.setStatus(MatchStatus.SCHEDULED);
        }
        auditService.record("UNPUBLISH", "Lineup", lineup.getId(), null, null, null);
        return present(match, lineup, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> current(Long matchId) {
        FootballMatch match = matchService.load(matchId);
        boolean hide = ClubAccess.publishedOnly();
        LineupVersion lineup = hide
                ? lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream()
                    .filter(version -> version.getStatus() == LineupState.PUBLISHED || version.getStatus() == LineupState.LOCKED || version.getStatus() == LineupState.FINISHED)
                    .findFirst().orElse(null)
                : lineupRepository.findByMatchIdOrderByRevisionDesc(matchId).stream().findFirst().orElse(null);
        if (lineup == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("matchId", matchId);
            empty.put("published", false);
            empty.put("slots", List.of());
            return empty;
        }
        return present(match, lineup, hide && lineup.getStatus() != LineupState.PUBLISHED);
    }

    private LineupVersion newVersion(FootballMatch match) {
        int next = lineupRepository.findByMatchIdOrderByRevisionDesc(match.getId()).stream()
                .map(LineupVersion::getRevision).findFirst().orElse(0) + 1;
        LineupVersion lineup = new LineupVersion();
        lineup.setMatch(match);
        lineup.setRevision(next);
        lineup.setStatus(LineupState.DRAFT);
        return lineup;
    }

    private void guardEditable(FootballMatch match) {
        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw BusinessException.conflict("A cancelled match has no lineup");
        }
        if (match.getKickoff().isBefore(Instant.now(clock)) && !SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can change a lineup after kickoff");
        }
    }

    private Map<String, Object> present(FootballMatch match, LineupVersion lineup, boolean hidden) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matchId", match.getId());
        body.put("lineupId", lineup.getId());
        body.put("revision", lineup.getRevision());
        body.put("status", lineup.getStatus());
        body.put("formation", lineup.getFormation());
        body.put("publishedAt", lineup.getPublishedAt());
        body.put("publishedBy", lineup.getPublishedBy());
        body.put("version", lineup.getVersion());
        body.put("deadlineAt", lineup.getDeadlineAt());
        body.put("submittedAt", lineup.getSubmittedAt());
        body.put("approvedAt", lineup.getApprovedAt());
        body.put("reviewComment", lineup.getReviewComment());
        body.put("editingBy", lineup.getEditingBy());
        body.put("editingUntil", lineup.getEditingUntil());
        if (hidden) {
            body.put("slots", List.of());
            return body;
        }
        Map<Long, CaptainRank> captains = new LinkedHashMap<>();
        captainRepository.findBySeasonIdAndEndedAtIsNullOrderByRankAsc(match.getSeason().getId())
                .forEach(captain -> captains.put(captain.getPlayer().getId(), captain.getRank()));
        Set<Long> selected = slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId()).stream()
                .map(slot -> slot.getPlayer().getId()).collect(Collectors.toSet());
        List<Map<String, Object>> slots = slotRepository.findByLineupIdOrderBySortOrderAsc(lineup.getId()).stream()
                .map(slot -> slotView(slot, captains.get(slot.getPlayer().getId()))).toList();
        body.put("slots", slots);
        body.put("notSelected", playerRepository.findByActiveTrueOrderByRegisterNoAsc().stream()
                .filter(player -> !selected.contains(player.getId()))
                .map(player -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("playerId", player.getId());
                    row.put("name", player.getKnownAs());
                    row.put("jersey", player.getJerseyNumber());
                    row.put("position", player.getPosition().getLabel());
                    row.put("status", player.getStatus() == PlayerStatus.INJURED ? "INJURED"
                            : player.getStatus() == PlayerStatus.SUSPENDED ? "SUSPENDED" : "NOT_SELECTED");
                    return row;
                }).toList());
        return body;
    }

    private Map<String, Object> slotView(LineupSlot slot, CaptainRank rank) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerId", slot.getPlayer().getId());
        body.put("name", slot.getPlayer().getKnownAs());
        body.put("fullName", slot.getPlayer().getFullName());
        body.put("jersey", slot.getPlayer().getJerseyNumber());
        body.put("photoUrl", slot.getPlayer().getPhotoUrl());
        body.put("position", slot.getPlayer().getPosition().getLabel());
        body.put("selection", slot.getSelection());
        body.put("positionSlot", slot.getPositionSlot());
        body.put("captain", slot.isCaptain());
        body.put("captainRank", rank);
        body.put("overrideReason", slot.getOverrideReason());
        body.put("x", slot.getCoordX());
        body.put("y", slot.getCoordY());
        return body;
    }

    public record SlotWrite(@NotNull Long playerId, @NotNull Selection selection, String positionSlot,
                            Integer sortOrder, Boolean captain, String overrideReason, Integer x, Integer y) {
    }

    public record LineupWrite(String formation, List<SlotWrite> slots, Long version,
                              String opponentFormation, String opponentNames) {
    }
}
