package rw.vikingfc.club.contract;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.exception.BusinessException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaptainRuleTest {

    @Test
    void teamCannotHaveMoreThanFourCaptainsOrADuplicateRank() {
        assertThrows(BusinessException.class,
                () -> CaptainRules.validate(4, false, Set.of(), CaptainRank.CAPTAIN, false));
        assertDoesNotThrow(() -> CaptainRules.validate(4, true, Set.of(), CaptainRank.CAPTAIN, false));
        assertThrows(BusinessException.class,
                () -> CaptainRules.validate(2, false, Set.of(CaptainRank.VICE_CAPTAIN), CaptainRank.VICE_CAPTAIN, true));
        assertDoesNotThrow(() -> CaptainRules.validate(3, false, Set.of(), CaptainRank.FOURTH_CAPTAIN, false));
    }

    @Test
    void movingOrReplacingACaptainDoesNotOpenAFifthSeat() {
        assertThrows(BusinessException.class, () -> CaptainRules.assertCanAdd(4, false));
        assertDoesNotThrow(() -> CaptainRules.assertCanAdd(4, true));
        assertDoesNotThrow(() -> CaptainRules.assertCanAdd(3, false));
    }
}
