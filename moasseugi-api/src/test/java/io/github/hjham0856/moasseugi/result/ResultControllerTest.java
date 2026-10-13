package io.github.hjham0856.moasseugi.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hjham0856.moasseugi.evaluation.EvaluationRepository;
import io.github.hjham0856.moasseugi.idea.IdeaRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantTokenGenerator;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 결과 분포 조회와 진행자의 1회 채택 규칙을 H2와 HTTP 응답으로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private IdeaRepository ideaRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ParticipantTokenGenerator participantTokenGenerator;

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
    void returnsDistributionInCreationOrderAndHidesAuthorsBeforeSelection() throws Exception {
        // given: 세 아이디어 중 둘만 평가를 받았고 안건은 RESULT이다.
        when(participantTokenGenerator.generate()).thenReturn("res-host", "res-first", "res-second");
        String sessionId = createSession();
        joinSession(sessionId, "첫 참가자");
        joinSession(sessionId, "둘째 참가자");
        String hostIdeaId = saveIdea(sessionId, "res-host", "진행자 생각");
        String firstIdeaId = saveIdea(sessionId, "res-first", "첫 참가자 생각");
        String secondIdeaId = saveIdea(sessionId, "res-second", "둘째 참가자 생각");
        setStatus(sessionId, "EVALUATING");
        putEvaluation(sessionId, "res-first", hostIdeaId, 1);
        putEvaluation(sessionId, "res-second", hostIdeaId, 0);
        putEvaluation(sessionId, "res-host", firstIdeaId, -1);
        putEvaluation(sessionId, "res-second", firstIdeaId, 1);
        setStatus(sessionId, "RESULT");

        // when: 참가 키 없이 결과를 조회한다.
        MvcResult result = mockMvc.perform(get("/sessions/{sessionId}/results", sessionId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        // then: 생성순이며 분포와 합계가 저장된 평가와 같고, 무평가는 0으로 포함되며 작성자는 숨겨진다.
        JsonNode ideas = body.path("ideas");
        assertEquals("RESULT", body.path("status").asText());
        assertTrue(body.path("selectedIdeaId").isNull());
        assertEquals(3, ideas.size());
        assertEquals(hostIdeaId, ideas.get(0).path("ideaId").asText());
        assertEquals(firstIdeaId, ideas.get(1).path("ideaId").asText());
        assertEquals(secondIdeaId, ideas.get(2).path("ideaId").asText());
        assertDistribution(ideas.get(0), 1, 1, 0, 2);
        assertDistribution(ideas.get(1), 1, 0, 1, 2);
        assertDistribution(ideas.get(2), 0, 0, 0, 0);
        for (JsonNode idea : ideas) {
            assertTrue(idea.path("authorNickname").isNull());
        }
    }

    @Test
    void rejectsResultsBeforeResultStage() throws Exception {
        // given: 안건이 아직 작성 중이다.
        when(participantTokenGenerator.generate()).thenReturn("res-stage-host");
        String sessionId = createSession();

        // when/then: 결과 단계가 아니면 조회가 거부된다.
        mockMvc.perform(get("/sessions/{sessionId}/results", sessionId))
                .andExpect(status().isConflict());
    }

    @Test
    void selectsIdeaWithoutEvaluationAndRevealsOnlyItsAuthor() throws Exception {
        // given: 평가를 받지 못한 아이디어가 있는 RESULT 안건이다.
        when(participantTokenGenerator.generate()).thenReturn("sel-host", "sel-guest");
        String sessionId = createSession();
        joinSession(sessionId, "참가자");
        saveIdea(sessionId, "sel-host", "진행자 생각");
        String guestIdeaId = saveIdea(sessionId, "sel-guest", "참가자 생각");
        setStatus(sessionId, "RESULT");

        // when: 진행자가 평가가 없는 아이디어를 채택한다.
        JsonNode body = selectIdea(sessionId, "sel-host", guestIdeaId, 200);

        // then: 단계는 RESULT를 유지하고 채택작의 작성자만 공개된다.
        assertEquals("RESULT", body.path("status").asText());
        assertEquals(guestIdeaId, body.path("selectedIdeaId").asText());
        for (JsonNode idea : body.path("ideas")) {
            boolean selected = guestIdeaId.equals(idea.path("ideaId").asText());
            assertEquals(selected, idea.path("selected").asBoolean());
            if (selected) {
                assertEquals("참가자", idea.path("authorNickname").asText());
                assertDistribution(idea, 0, 0, 0, 0);
            } else {
                assertTrue(idea.path("authorNickname").isNull());
            }
        }
    }

    @Test
    void rejectsSelectionByNonHost() throws Exception {
        // given: 일반 참가자가 있는 RESULT 안건이다.
        when(participantTokenGenerator.generate()).thenReturn("sel-perm-host", "sel-perm-guest");
        String sessionId = createSession();
        joinSession(sessionId, "참가자");
        String ideaId = saveIdea(sessionId, "sel-perm-guest", "참가자 생각");
        setStatus(sessionId, "RESULT");

        // when/then: 진행자가 아니면 채택할 수 없다.
        selectIdea(sessionId, "sel-perm-guest", ideaId, 403);
    }

    @Test
    void rejectsSelectingIdeaOfAnotherSession() throws Exception {
        // given: 다른 안건에 속한 아이디어가 있다.
        when(participantTokenGenerator.generate()).thenReturn("sel-own-host", "sel-foreign-host");
        String sessionId = createSession();
        String otherSessionId = createSession();
        String foreignIdeaId = saveIdea(otherSessionId, "sel-foreign-host", "다른 안건 생각");
        setStatus(sessionId, "RESULT");

        // when/then: 이 안건에서 찾을 수 없는 아이디어이므로 404이다.
        selectIdea(sessionId, "sel-own-host", foreignIdeaId, 404);
    }

    @Test
    void rejectsSelectingAgainOnceSelected() throws Exception {
        // given: 이미 하나를 채택한 RESULT 안건이다.
        when(participantTokenGenerator.generate()).thenReturn("sel-twice-host", "sel-twice-guest");
        String sessionId = createSession();
        joinSession(sessionId, "참가자");
        String hostIdeaId = saveIdea(sessionId, "sel-twice-host", "진행자 생각");
        String guestIdeaId = saveIdea(sessionId, "sel-twice-guest", "참가자 생각");
        setStatus(sessionId, "RESULT");
        selectIdea(sessionId, "sel-twice-host", hostIdeaId, 200);

        // when/then: 다른 아이디어로 바꾸려는 요청은 거부되고 첫 채택이 유지된다.
        selectIdea(sessionId, "sel-twice-host", guestIdeaId, 409);
        JsonNode body = objectMapper.readTree(mockMvc.perform(get("/sessions/{sessionId}/results", sessionId))
                .andReturn().getResponse().getContentAsString());
        assertEquals(hostIdeaId, body.path("selectedIdeaId").asText());
    }

    private void assertDistribution(JsonNode idea, int positive, int neutral, int negative, int total) {
        JsonNode distribution = idea.path("distribution");
        assertEquals(positive, distribution.path("positive").asInt());
        assertEquals(neutral, distribution.path("neutral").asInt());
        assertEquals(negative, distribution.path("negative").asInt());
        assertEquals(total, distribution.path("total").asInt());
    }

    private void clearRecords() {
        jdbcTemplate.update("UPDATE sessions SET selected_idea_id = NULL");
        evaluationRepository.deleteAll();
        ideaRepository.deleteAll();
        participantRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    private String createSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("session").path("id").asText();
    }

    private void joinSession(String sessionId, String nickname) throws Exception {
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", nickname))))
                .andExpect(status().isCreated());
    }

    private String saveIdea(String sessionId, String token, String content) throws Exception {
        MvcResult result = mockMvc.perform(put("/sessions/{sessionId}/ideas", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }

    private void setStatus(String sessionId, String status) {
        jdbcTemplate.update("UPDATE sessions SET status = ? WHERE id = ?", status, UUID.fromString(sessionId));
    }

    private void putEvaluation(String sessionId, String token, String ideaId, int value) throws Exception {
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ideaId", ideaId, "value", value))))
                .andExpect(status().isOk());
    }

    private JsonNode selectIdea(String sessionId, String token, String ideaId, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/sessions/{sessionId}/select-idea", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ideaId", ideaId))))
                .andExpect(status().is(expectedStatus))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
