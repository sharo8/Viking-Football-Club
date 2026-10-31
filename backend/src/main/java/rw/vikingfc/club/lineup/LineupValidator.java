package rw.vikingfc.club.lineup;

import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.player.Position;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LineupValidator {

    private LineupValidator() {
    }

    public record SlotInput(Long playerId, Selection selection, String positionSlot, String overrideReason) {
    }

    public static void validateDraft(List<SlotInput> slots, List<Player> players, Set<Long> unavailableIds) {
        Set<Long> seen = new HashSet<>();
        for (SlotInput slot : slots) {
            if (!seen.add(slot.playerId())) {
                throw BusinessException.conflict("A player cannot appear twice in the lineup");
            }
            Player player = players.stream().filter(item -> item.getId().equals(slot.playerId())).findFirst()
                    .orElseThrow(() -> BusinessException.unprocessable("Unknown player in lineup: " + slot.playerId()));
            if (player.getStatus() == PlayerStatus.RELEASED || player.getStatus() == PlayerStatus.TRANSFERRED) {
                throw BusinessException.unprocessable(player.getFullName() + " is no longer in the squad", "PLAYER_UNAVAILABLE");
            }
            boolean blocked = player.getStatus() == PlayerStatus.INJURED
                    || player.getStatus() == PlayerStatus.SUSPENDED
                    || unavailableIds.contains(player.getId());
            if (blocked && (slot.overrideReason() == null || slot.overrideReason().isBlank()) && !rw.vikingfc.club.common.util.SecurityUtils.isExecutive()) {
                throw BusinessException.unprocessable(player.getFullName() + " is unavailable and needs an explicit override reason", "PLAYER_UNAVAILABLE");
            }
        }
    }

    public static void validate(List<SlotInput> slots, List<Player> players, int maxSubstitutes, Set<Long> unavailableIds) {
        long starters = slots.stream().filter(slot -> slot.selection() == Selection.STARTING).count();
        long bench = slots.stream().filter(slot -> slot.selection() == Selection.SUBSTITUTE).count();
        if (starters != 11) {
            throw BusinessException.unprocessable("A lineup needs exactly 11 starters");
        }
        if (bench > maxSubstitutes) {
            throw BusinessException.unprocessable("A lineup allows at most " + maxSubstitutes + " substitutes");
        }
        Set<Long> seen = new HashSet<>();
        for (SlotInput slot : slots) {
            if (!seen.add(slot.playerId())) {
                throw BusinessException.conflict("A player cannot appear twice in the lineup");
            }
        }
        boolean keeper = false;
        for (SlotInput slot : slots) {
            Player player = players.stream().filter(item -> item.getId().equals(slot.playerId())).findFirst()
                    .orElseThrow(() -> BusinessException.unprocessable("Unknown player in lineup: " + slot.playerId()));
            if (slot.selection() == Selection.STARTING && player.getPosition() == Position.GOALKEEPER) {
                keeper = true;
            }
            boolean blocked = player.getStatus() == PlayerStatus.INJURED
                    || player.getStatus() == PlayerStatus.SUSPENDED
                    || unavailableIds.contains(player.getId());
            if (blocked && (slot.overrideReason() == null || slot.overrideReason().isBlank())) {
                throw BusinessException.unprocessable(
                        player.getFullName() + " is unavailable and needs an explicit override reason");
            }
        }
        if (!keeper) {
            throw BusinessException.unprocessable("The starting XI must include a goalkeeper");
        }
    }
}
