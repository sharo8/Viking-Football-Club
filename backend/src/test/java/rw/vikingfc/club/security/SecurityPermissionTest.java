package rw.vikingfc.club.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityPermissionTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void executivesShareFinanceAccessAndOtherRolesDoNot() throws Exception {
        String president = token("president@vikingfc.rw");
        String manager = token("generalmanager@vikingfc.rw");
        String finance = token("finance@vikingfc.rw");
        String coach = token("coach@vikingfc.rw");
        String doctor = token("doctor@vikingfc.rw");

        mockMvc.perform(get("/api/finance/summary").header("Authorization", "Bearer " + president))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players").value(25))
                .andExpect(jsonPath("$.data.seasonPayroll").value(14040000))
                .andExpect(jsonPath("$.data.monthlyWageBill").value(1755000))
                .andExpect(jsonPath("$.data.averageSeasonalPay").value(561600))
                .andExpect(jsonPath("$.data.medianSeasonalPay").value(480000))
                .andExpect(jsonPath("$.data.advances").value(1960000))
                .andExpect(jsonPath("$.data.playersWithAdvance").value(17))
                .andExpect(jsonPath("$.data.envelopes").value(3041000))
                .andExpect(jsonPath("$.data.socialFund").value(576000))
                .andExpect(jsonPath("$.data.socialFundExempt").value(1))
                .andExpect(jsonPath("$.data.netBalanceDue").value(8463000))
                .andExpect(jsonPath("$.data.paidToDate").value(5001000))
                .andExpect(jsonPath("$.data.workbookCheckMatchesPayroll").value(true));

        String managerBody = body(manager);
        String financeBody = body(finance);
        assertEquals(managerBody, financeBody);

        mockMvc.perform(get("/api/finance/summary").header("Authorization", "Bearer " + coach))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/finance/unpaid").header("Authorization", "Bearer " + doctor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/players/1").header("Authorization", "Bearer " + coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seasonalPay").doesNotExist());

        mockMvc.perform(get("/api/attendance/kpis").header("Authorization", "Bearer " + coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendancePercent").value(52.0))
                .andExpect(jsonPath("$.data.absent").value(13));
        mockMvc.perform(get("/api/attendance/at-risk").header("Authorization", "Bearer " + coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(15));
        mockMvc.perform(get("/api/fines/summary").header("Authorization", "Bearer " + president))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFinesDue").value(52000))
                .andExpect(jsonPath("$.data.playersFined").value(9));
    }

    private String token(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"ChangeMe@2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("accessToken").asText();
    }

    private String body(String token) throws Exception {
        String json = mockMvc.perform(get("/api/finance/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).get("data");
        return data.get("seasonPayroll").asText() + data.get("netBalanceDue").asText();
    }
}
