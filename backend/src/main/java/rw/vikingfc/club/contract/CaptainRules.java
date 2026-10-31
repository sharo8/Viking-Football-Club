package rw.vikingfc.club.contract;

import rw.vikingfc.club.common.exception.BusinessException;

import java.util.Set;

public final class CaptainRules {

    public static final int MAX_CAPTAINS = 4;

    private CaptainRules() {
    }

    public static void validate(int currentCount, boolean replacingSamePlayer, Set<CaptainRank> usedRanks,
                                CaptainRank rank, boolean rankTakenBySomeoneElse) {
        if (!replacingSamePlayer && currentCount >= MAX_CAPTAINS) {
            throw BusinessException.conflict("The team already has 4 captains for this season");
        }
        if (rankTakenBySomeoneElse) {
            throw BusinessException.conflict("Rank " + rank + " is already assigned");
        }
        if (usedRanks.contains(rank) && rankTakenBySomeoneElse) {
            throw BusinessException.conflict("Each rank can be held by only one player");
        }
    }

    public static void assertCanAdd(int openSlots, boolean reusesSlot) {
        if (!reusesSlot && openSlots >= MAX_CAPTAINS) {
            throw BusinessException.conflict("The team already has 4 captains for this season", "CAPTAIN_LIMIT", null);
        }
    }
}
