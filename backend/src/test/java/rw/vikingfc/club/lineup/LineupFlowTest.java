package rw.vikingfc.club.lineup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LineupFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void coachPublishesElevenAndAGoalUpdatesTheScorer() throws Exception {
        String coach = token("coach@vikingfc.rw");
        JsonNode players = json(fetch("/api/players", coach)).get("data");
        long keeper = -1;
        ArrayNode slots = objectMapper.createArrayNode();
        int field = 0;
        for (JsonNode player : players) {
            boolean isKeeper = "Goalkeeper".equals(player.get("position").asText());
            if (isKeeper && keeper < 0) {
                keeper = player.get("id").asLong();
                slots.add(slot(keeper, "GK"));
            } else if (!isKeeper && field < 10) {
                slots.add(slot(player.get("id").asLong(), "SLOT"));
                field++;
            }
        }
        JsonNode matches = json(fetch("/api/matches", coach)).get("data");
        long matchId = matches.get(0).get("id").asLong();
        ObjectNode lineup = objectMapper.createObjectNode();
        lineup.put("formation", "4-3-3");
        lineup.set("slots", slots);

        mockMvc.perform(put("/api/matches/" + matchId + "/lineup")
                        .header("Authorization", "Bearer " + coach)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots.length()").value(11));

        mockMvc.perform(post("/api/matches/" + matchId + "/lineup/publish")
                        .header("Authorization", "Bearer " + coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        long scorer = slots.get(1).get("playerId").asLong();
        ObjectNode event = objectMapper.createObjectNode();
        event.put("minute", 23);
        event.put("type", "GOAL");
        event.put("playerId", scorer);
        event.put("relatedPlayerId", slots.get(2).get("playerId").asLong());
        mockMvc.perform(post("/api/matches/" + matchId + "/events")
                        .header("Authorization", "Bearer " + coach)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goalsFor").value(1));

        mockMvc.perform(get("/api/players/" + scorer + "/stats").header("Authorization", "Bearer " + coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goals").value(1))
                .andExpect(jsonPath("$.data.bonusEarned").doesNotExist());

        String doctor = token("doctor@vikingfc.rw");
        ObjectNode injury = objectMapper.createObjectNode();
        injury.put("playerId", scorer);
        injury.put("injuryType", "Hamstring");
        injury.put("diagnosis", "Grade 1 strain");
        injury.put("bodyPart", "Hamstring");
        injury.put("severity", "MINOR");
        injury.put("cause", "MATCH");
        injury.put("injuryDate", "2026-10-24");
        injury.put("expectedReturnDate", "2026-11-07");
        mockMvc.perform(post("/api/medical/injuries")
                        .header("Authorization", "Bearer " + doctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(injury)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/medical/dashboard").header("Authorization", "Bearer " + doctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unavailable").value(1));
        mockMvc.perform(get("/api/medical/availability").header("Authorization", "Bearer " + coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.playerId == " + scorer + ")].availability").value("INJURED"))
                .andExpect(jsonPath("$.data[?(@.playerId == " + scorer + ")].diagnosis").doesNotExist());
    }

    private ObjectNode slot(long playerId, String positionSlot) {
        ObjectNode slot = objectMapper.createObjectNode();
        slot.put("playerId", playerId);
        slot.put("selection", "STARTING");
        slot.put("positionSlot", positionSlot);
        slot.put("captain", "GK".equals(positionSlot));
        return slot;
    }

    private String token(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"ChangeMe@2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).get("data");
        String access = data.get("accessToken").asText();
        if (!data.path("mustChangePassword").asBoolean(false)) {
            return access;
        }
        String changed = mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"ChangeMe@2026\",\"newPassword\":\"Viking#2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(changed).get("data").get("accessToken").asText();
    }

    private String fetch(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private JsonNode json(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}
