package rw.vikingfc.club.removal;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemovalRulesTest {

    @Test
    void protectedRolesCannotBeDeleted() {
        assertThrows(BusinessException.class, () -> RemovalRules.roleMayBeDeleted("PRESIDENT"));
        assertThrows(BusinessException.class, () -> RemovalRules.roleMayBeDeleted("PLAYER"));
        RemovalRules.roleMayBeDeleted("KIT_MANAGER");
    }

    @Test
    void moveMustStayInTheSameCategory() {
        BusinessException ex = assertThrows(BusinessException.class, () -> RemovalRules.sameCategory("STAFF", "ADMINISTRATION"));
        assertEquals("ROLE_CATEGORY_MISMATCH", ex.getCode());
        RemovalRules.sameCategory("Staff", "STAFF");
    }

    @Test
    void youCannotTouchYourOwnAccount() {
        BusinessException ex = assertThrows(BusinessException.class, () -> RemovalRules.notSelf(4L, 4L));
        assertEquals("SELF", ex.getCode());
        RemovalRules.notSelf(4L, 5L);
    }

    @Test
    void seatsAreEnforced() {
        BusinessException ex = assertThrows(BusinessException.class, () -> RemovalRules.seats(1, 1, 1));
        assertEquals("ROLE_SEATS_FULL", ex.getCode());
        RemovalRules.seats(null, 9, 3);
        RemovalRules.seats(2, 1, 1);
    }

    @Test
    void theLastExecutiveStays() {
        BusinessException ex = assertThrows(BusinessException.class, () -> RemovalRules.notLastExecutive("PRESIDENT", 1));
        assertEquals("LAST_EXECUTIVE", ex.getCode());
        RemovalRules.notLastExecutive("PRESIDENT", 2);
        RemovalRules.notLastExecutive("KIT_MANAGER", 1);
    }

    @Test
    void aReasonIsRequired() {
        assertThrows(BusinessException.class, () -> RemovalRules.reason(null, "long enough text"));
        assertThrows(BusinessException.class, () -> RemovalRules.reason("OTHER", "short"));
        assertThrows(BusinessException.class, () -> RemovalRules.reason("MISTAKE", "tiny"));
        RemovalRules.reason("MISTAKE", "");
        RemovalRules.reason("OTHER", "Created the wrong account");
    }

    @Test
    void votesCloseTheRequest() {
        assertEquals("PENDING", RemovalRules.afterVotes(false, 1));
        assertEquals("PENDING", RemovalRules.afterVotes(false, 2));
        assertEquals("APPROVED", RemovalRules.afterVotes(false, 3));
        assertEquals("REJECTED", RemovalRules.afterVotes(true, 2));
    }

    @Test
    void deletionRequestGuards() {
        assertThrows(BusinessException.class, () -> RemovalRules.deletionReason("too short"));
        RemovalRules.deletionReason("This player never played and the record was created by mistake.");
        assertThrows(BusinessException.class, () -> RemovalRules.openRequest(true));
        assertThrows(BusinessException.class, () -> RemovalRules.formerOnly(false));
        assertThrows(BusinessException.class, () -> RemovalRules.executivesPresent(1, 1, 0));
        assertThrows(BusinessException.class, () -> RemovalRules.oneVote(true));
        RemovalRules.executivesPresent(1, 1, 1);
        RemovalRules.confirmName("Eric Kit", "eric kit");
        assertThrows(BusinessException.class, () -> RemovalRules.confirmName("Eric Kit", "Eric"));
    }
}
