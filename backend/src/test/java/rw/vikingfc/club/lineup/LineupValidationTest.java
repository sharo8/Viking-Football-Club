package rw.vikingfc.club.lineup;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerStatus;
import rw.vikingfc.club.player.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineupValidationTest {

    @Test
    void requiresElevenStartersAGoalkeeperAndBlocksUnavailablePlayers() {
        List<Player> players = new ArrayList<>();
        List<LineupValidator.SlotInput> slots = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            Player player = player(i, i == 1 ? Position.GOALKEEPER : Position.FORWARD);
            players.add(player);
            slots.add(new LineupValidator.SlotInput((long) i, Selection.STARTING, "SLOT", null));
        }
        LineupValidator.validate(slots, players, 9, Set.of());

        Player injured = player(12, Position.FORWARD);
        injured.setStatus(PlayerStatus.INJURED);
        players.add(injured);
        slots.set(10, new LineupValidator.SlotInput(12L, Selection.STARTING, "ST", null));
        BusinessException blocked = assertThrows(BusinessException.class,
                () -> LineupValidator.validate(slots, players, 9, Set.of()));
        assertTrue(blocked.getMessage().contains("override"));
    }

    private Player player(int id, Position position) {
        Player player = new Player();
        player.setId((long) id);
        player.setFullName("Player " + id);
        player.setPosition(position);
        player.setStatus(PlayerStatus.ACTIVE);
        return player;
    }
}
