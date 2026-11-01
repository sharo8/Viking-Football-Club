package rw.vikingfc.club.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePolicyTest {

    @Test
    void otpIsRequiredForTheSixSeededRolesAndNeverForAPlayer() {
        assertTrue(RolePolicy.otpRequired("PRESIDENT"));
        assertTrue(RolePolicy.otpRequired("GENERAL_MANAGER"));
        assertTrue(RolePolicy.otpRequired("FINANCE_MANAGER"));
        assertTrue(RolePolicy.otpRequired("HEAD_COACH"));
        assertTrue(RolePolicy.otpRequired("DOCTOR"));
        assertTrue(RolePolicy.otpRequired("KIT_MANAGER"));
        assertFalse(RolePolicy.otpRequired("PLAYER"));
        assertFalse(RolePolicy.otpRequired("ASSISTANT_MANAGER"));
        assertFalse(RolePolicy.otpRequired("TEAM_MANAGER"));
    }

    @Test
    void financeAndAssistantManagersAreAdministrationNotStaff() {
        assertEquals("ADMINISTRATION", RolePolicy.category(RolePolicy.codeOf("Finance Manager")));
        assertEquals("ADMINISTRATION", RolePolicy.category(RolePolicy.codeOf("Assistant Manager")));
        assertEquals("STAFF", RolePolicy.category(RolePolicy.codeOf("Head Coach")));
        assertEquals("STAFF", RolePolicy.category(RolePolicy.codeOf("Team Manager")));
        assertEquals(1, RolePolicy.seats("PRESIDENT"));
    }

    @Test
    void onlyTheFourProtectedRolesStayLocked() {
        assertTrue(RolePolicy.locked("PRESIDENT"));
        assertTrue(RolePolicy.locked("GENERAL_MANAGER"));
        assertTrue(RolePolicy.locked("FINANCE_MANAGER"));
        assertTrue(RolePolicy.locked("PLAYER"));
        assertFalse(RolePolicy.locked("HEAD_COACH"));
        assertFalse(RolePolicy.locked("DOCTOR"));
        assertFalse(RolePolicy.locked("PHYSIOTHERAPIST"));
        assertFalse(RolePolicy.locked("KIT_MANAGER"));
    }
}
