package rw.vikingfc.club.staff;

import org.junit.jupiter.api.Test;
import rw.vikingfc.club.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaffAccountRulesTest {

    @Test
    void statusFollowsTheAccount() {
        assertEquals("NONE", StaffAccountRules.status(false, true, true, false));
        assertEquals("SUSPENDED", StaffAccountRules.status(true, false, false, true));
        assertEquals("INVITED", StaffAccountRules.status(true, true, true, false));
        assertEquals("INVITED", StaffAccountRules.status(true, true, false, false));
        assertEquals("ACTIVE", StaffAccountRules.status(true, true, false, true));
        assertEquals("ACTIVE", StaffAccountRules.status(true, true, true, true));
    }

    @Test
    void administrationRoleIsRejected() {
        BusinessException error = assertThrows(BusinessException.class, () -> StaffAccountRules.staffCategory("PRESIDENT"));
        assertEquals("ROLE_CATEGORY_MISMATCH", error.getCode());
    }

    @Test
    void staffRoleIsAccepted() {
        StaffAccountRules.staffCategory("KIT_MANAGER");
    }

    @Test
    void duplicateEmailAndBadEmailAreRejected() {
        assertEquals("EMAIL_TAKEN", assertThrows(BusinessException.class, () -> StaffAccountRules.unique(true)).getCode());
        assertEquals("EMAIL_INVALID", assertThrows(BusinessException.class, () -> StaffAccountRules.email("not-an-email")).getCode());
    }

    @Test
    void secondHeadCoachIsRejected() {
        assertEquals("ROLE_SEATS_FULL", assertThrows(BusinessException.class, () -> StaffAccountRules.headCoachSeat("HEAD_COACH", 1, false)).getCode());
        StaffAccountRules.headCoachSeat("HEAD_COACH", 1, true);
        StaffAccountRules.headCoachSeat("DOCTOR", 4, false);
    }

    @Test
    void removeWithoutAReasonIsRejected() {
        assertEquals("REASON_REQUIRED", assertThrows(BusinessException.class, () -> StaffAccountRules.reason(null, null)).getCode());
        StaffAccountRules.reason("MISTAKE", "");
    }

    @Test
    void archivedStaffCannotGetAnAccount() {
        assertEquals("ARCHIVED", assertThrows(BusinessException.class, () -> StaffAccountRules.notArchived(false)).getCode());
    }
}
