package io.github.hjham0856.moasseugi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기능별 규칙이 아니라 실제 흐름에서 API들이 서로 이어지는지 확인한다.
 * 기능별 규칙은 각 기능의 테스트가 검증하므로, 여기서는 단계 전환 API로 실제 단계를 바꾼 뒤
 * 다른 API의 허용·거부와 저장 결과가 연결되는지만 본다. 참가 키는 실제 발급기가 만든 값을 응답에서 받아 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoreFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 채택한 아이디어를 가리키는 안건 참조를 먼저 풀어 외래 키 순서를 지킨다.
    @BeforeEach
    void clearStoredRecords() {
        clearRecords();
    }

    @AfterEach
    void removeStoredRecords() {
        clearRecords();
    }

    @Test
    void walksFromCreationThroughEvaluationToSingleSelection() throws Exception {
        // given: 로그인 없이 안건을 만든 진행자와 닉네임으로 참가한 두 참가자가 있다.
        JsonNode created = createSession();
        String sessionId = created.path("session").path("id").asText();
        String hostToken = created.path("participantToken").asText();
        String firstToken = join(sessionId, "첫 참가자");
        String secondToken = join(sessionId, "둘째 참가자");

        // when: 각자 아이디어를 저장하고(한 명은 수정), 진행자가 평가를 시작한다.
        String hostIdeaId = saveIdea(sessionId, hostToken, "진행자 생각");
        String firstIdeaId = saveIdea(sessionId, firstToken, "첫 참가자 생각");
        String secondIdeaId = saveIdea(sessionId, secondToken, "수정 전 생각");
        assertEquals(secondIdeaId, saveIdea(sessionId, secondToken, "둘째 참가자 생각"));
        transition(sessionId, "close-writing", hostToken, 200);

        // when: 평가를 남기고 하나를 바꾼 뒤 진행자가 평가를 종료해 결과를 조회한다.
        mockMvc.perform(get("/sessions/{sessionId}/evaluation-items", sessionId)
                        .header("X-Participant-Token", firstToken))
                .andExpect(status().isOk());
        evaluate(sessionId, firstToken, hostIdeaId, 1, 200);
        evaluate(sessionId, secondToken, hostIdeaId, 0, 200);
        evaluate(sessionId, hostToken, firstIdeaId, -1, 200);
        evaluate(sessionId, secondToken, firstIdeaId, -1, 200);
        evaluate(sessionId, secondToken, firstIdeaId, 1, 200);
        transition(sessionId, "close-evaluation", hostToken, 200);
        JsonNode results = json(mockMvc.perform(get("/sessions/{sessionId}/results", sessionId))
                .andExpect(status().isOk()).andReturn());

        // then: 분포는 저장된 평가와 같고 무평가 아이디어도 포함되며 작성자는 숨겨지고, 평가는 참가자·아이디어당 한 건이다.
        assertEquals("RESULT", results.path("status").asText());
        assertEquals(3, results.path("ideas").size());
        assertDistribution(results.path("ideas").get(0), 1, 1, 0, 2);
        assertDistribution(results.path("ideas").get(1), 1, 0, 1, 2);
        assertDistribution(results.path("ideas").get(2), 0, 0, 0, 0);
        assertEquals(secondIdeaId, results.path("ideas").get(2).path("ideaId").asText());
        for (JsonNode idea : results.path("ideas")) {
            assertTrue(idea.path("authorNickname").isNull());
        }
        assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evaluations", Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT DISTINCT participant_id, idea_id FROM evaluations) t", Integer.class));

        // when: 진행자가 하나를 채택한다.
        JsonNode selected = json(mockMvc.perform(post("/sessions/{sessionId}/select-idea", sessionId)
                        .header("X-Participant-Token", hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ideaId", firstIdeaId))))
                .andExpect(status().isOk()).andReturn());
        JsonNode session = json(mockMvc.perform(get("/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk()).andReturn());

        // then: 채택작의 작성자만 공개되고 안건은 RESULT로 남아 공개 조회에도 채택이 반영된다.
        assertEquals("RESULT", selected.path("status").asText());
        for (JsonNode idea : selected.path("ideas")) {
            boolean isSelected = firstIdeaId.equals(idea.path("ideaId").asText());
            assertEquals(isSelected, !idea.path("authorNickname").isNull());
        }
        assertEquals(firstIdeaId, session.path("selectedIdeaId").asText());
        assertEquals("RESULT", session.path("status").asText());
    }

    @Test
    void rejectsJoinAndIdeaSaveAfterClosingWritingThroughTransitionApi() throws Exception {
        // given: 진행자가 아이디어를 저장한 뒤 전환 API로 작성을 마감했다.
        JsonNode created = createSession();
        String sessionId = created.path("session").path("id").asText();
        String hostToken = created.path("participantToken").asText();
        saveIdea(sessionId, hostToken, "진행자 생각");
        transition(sessionId, "close-writing", hostToken, 200);

        // when/then: 전환 이후 신규 참가와 아이디어 저장은 거부되고, 기존 참가자는 평가 대상을 계속 조회한다.
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"늦은 참가자\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/sessions/{sessionId}/ideas", sessionId)
                        .header("X-Participant-Token", hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"뒤늦은 수정\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/sessions/{sessionId}/evaluation-items", sessionId)
                        .header("X-Participant-Token", hostToken))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsEvaluationAfterClosingEvaluationThroughTransitionApi() throws Exception {
        // given: 평가 중 저장된 평가가 있고 진행자가 전환 API로 평가를 종료했다.
        JsonNode created = createSession();
        String sessionId = created.path("session").path("id").asText();
        String hostToken = created.path("participantToken").asText();
        String guestToken = join(sessionId, "참가자");
        String hostIdeaId = saveIdea(sessionId, hostToken, "진행자 생각");
        transition(sessionId, "close-writing", hostToken, 200);
        evaluate(sessionId, guestToken, hostIdeaId, 1, 200);
        transition(sessionId, "close-evaluation", hostToken, 200);

        // when/then: 종료 이후 평가 변경은 거부되고 저장된 평가는 그대로다.
        evaluate(sessionId, guestToken, hostIdeaId, -1, 409);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT \"value\" FROM evaluations", Integer.class));
    }

    private void clearRecords() {
        jdbcTemplate.update("UPDATE sessions SET selected_idea_id = NULL");
        jdbcTemplate.update("DELETE FROM evaluations");
        jdbcTemplate.update("DELETE FROM ideas");
        jdbcTemplate.update("DELETE FROM participants");
        jdbcTemplate.update("DELETE FROM sessions");
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createSession() throws Exception {
        return json(mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated()).andReturn());
    }

    private String join(String sessionId, String nickname) throws Exception {
        return json(mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", nickname))))
                .andExpect(status().isCreated()).andReturn()).path("participantToken").asText();
    }

    private String saveIdea(String sessionId, String token, String content) throws Exception {
        return json(mockMvc.perform(put("/sessions/{sessionId}/ideas", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isOk()).andReturn()).path("id").asText();
    }

    private void transition(String sessionId, String action, String token, int expectedStatus) throws Exception {
        mockMvc.perform(post("/sessions/{sessionId}/{action}", sessionId, action)
                        .header("X-Participant-Token", token))
                .andExpect(status().is(expectedStatus));
    }

    private void evaluate(String sessionId, String token, String ideaId, int value, int expectedStatus)
            throws Exception {
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ideaId", ideaId, "value", value))))
                .andExpect(status().is(expectedStatus));
    }

    private void assertDistribution(JsonNode idea, int positive, int neutral, int negative, int total) {
        JsonNode distribution = idea.path("distribution");
        assertEquals(positive, distribution.path("positive").asInt());
        assertEquals(neutral, distribution.path("neutral").asInt());
        assertEquals(negative, distribution.path("negative").asInt());
        assertEquals(total, distribution.path("total").asInt());
    }
}
