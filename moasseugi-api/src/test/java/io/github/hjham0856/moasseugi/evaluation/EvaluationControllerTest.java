package io.github.hjham0856.moasseugi.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hjham0856.moasseugi.idea.IdeaEntity;
import io.github.hjham0856.moasseugi.idea.IdeaRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.participant.ParticipantRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantTokenGenerator;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 익명 평가 목록과 클릭 단위 저장 규칙을 H2와 HTTP 응답으로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EvaluationControllerTest {

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

    // 이전 검증의 평가부터 제거해 외래 키를 지킨다.
    @BeforeEach
    void clearStoredEvaluations() {
        clearRecords();
    }

    @AfterEach
    void removeStoredEvaluations() {
        clearRecords();
    }

    @Test
    void listsAnonymousIdeasAndSavesEachClickAsTheCurrentEvaluation() throws Exception {
        // given: 세 참가자가 각자 아이디어를 저장하고 안건이 평가 단계에 있다.
        when(participantTokenGenerator.generate()).thenReturn("eval-host", "eval-first", "eval-second");
        String sessionId = createSession();
        joinSession(sessionId, "첫 참가자");
        joinSession(sessionId, "둘째 참가자");
        String hostIdeaId = saveIdea(sessionId, "eval-host", "진행자 생각");
        String firstIdeaId = saveIdea(sessionId, "eval-first", "첫 참가자 생각");
        String secondIdeaId = saveIdea(sessionId, "eval-second", "둘째 참가자 생각");
        setEvaluating(sessionId);

        // when: 본인 평가 전 목록을 읽고, 다른 두 아이디어를 각각 평가한 뒤 첫 선택을 바꾼다.
        JsonNode before = readItems(sessionId, "eval-host");
        putEvaluation(sessionId, "eval-host", firstIdeaId, 1);
        putEvaluation(sessionId, "eval-host", firstIdeaId, -1);
        putEvaluation(sessionId, "eval-host", secondIdeaId, 0);
        JsonNode after = readItems(sessionId, "eval-host");

        // then: 본인 아이디어와 작성자 정보는 빠지고, 미평가 null·중립 0·변경값이 구분된다.
        assertEquals(2, before.size());
        assertEquals(2, after.size());
        Map<String, JsonNode> itemsByIdea = indexByIdeaId(after);
        assertFalse(itemsByIdea.containsKey(hostIdeaId));
        assertTrue(before.findValues("myEvaluation").stream().allMatch(JsonNode::isNull));
        assertEquals(-1, itemsByIdea.get(firstIdeaId).path("myEvaluation").asInt());
        assertEquals(0, itemsByIdea.get(secondIdeaId).path("myEvaluation").asInt());
        assertEquals(2, evaluationRepository.count());

        ParticipantEntity host = participantRepository.findBySession_IdAndParticipantToken(
                UUID.fromString(sessionId), "eval-host").orElseThrow();
        IdeaEntity firstIdea = ideaRepository.findById(UUID.fromString(firstIdeaId)).orElseThrow();
        assertThrows(DataIntegrityViolationException.class,
                () -> evaluationRepository.saveAndFlush(new EvaluationEntity(host, firstIdea, (short) 0)));
        assertEquals(2, evaluationRepository.count());

        for (JsonNode item : after) {
            assertFalse(item.has("participantId"));
            assertFalse(item.has("nickname"));
            assertEquals(3, item.size());
        }
    }

    @Test
    void rejectsEvaluationAccessOutsideEvaluating() throws Exception {
        // given: 참가자 둘이 아이디어를 저장했지만 안건은 아직 작성 중이다.
        when(participantTokenGenerator.generate()).thenReturn("eval-stage-host", "eval-stage-guest");
        String sessionId = createSession();
        joinSession(sessionId, "참가자");
        String ideaId = saveIdea(sessionId, "eval-stage-guest", "참가자 생각");
        saveIdea(sessionId, "eval-stage-host", "진행자 생각");

        // when/then: 작성 중에는 평가를 시작할 수 없고, RESULT가 되면 기존 평가도 바꿀 수 없다.
        mockMvc.perform(get("/sessions/{sessionId}/evaluation-items", sessionId)
                        .header("X-Participant-Token", "eval-stage-host"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", "eval-stage-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest(ideaId, 1)))
                .andExpect(status().isConflict());

        setEvaluating(sessionId);
        putEvaluation(sessionId, "eval-stage-host", ideaId, 1);
        jdbcTemplate.update("UPDATE sessions SET status = ? WHERE id = ?", "RESULT", UUID.fromString(sessionId));
        mockMvc.perform(get("/sessions/{sessionId}/evaluation-items", sessionId)
                        .header("X-Participant-Token", "eval-stage-host"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", "eval-stage-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest(ideaId, -1)))
                .andExpect(status().isConflict());

        assertEquals(1, evaluationRepository.count());
        assertEquals(1, evaluationRepository.findAll().getFirst().getValue());
    }

    @Test
    void rejectsEvaluatingOwnIdea() throws Exception {
        // given: 평가 단계의 진행자가 자기 아이디어를 가지고 있다.
        when(participantTokenGenerator.generate()).thenReturn("eval-self-host");
        String sessionId = createSession();
        String ideaId = saveIdea(sessionId, "eval-self-host", "내 생각");
        setEvaluating(sessionId);

        // when/then: 목록은 비어 있고 자기 아이디어 평가는 403으로 행을 만들지 않는다.
        assertEquals(0, readItems(sessionId, "eval-self-host").size());
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", "eval-self-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest(ideaId, 1)))
                .andExpect(status().isForbidden());
        assertEquals(0, evaluationRepository.count());
    }

    @Test
    void rejectsIdeaFromAnotherSession() throws Exception {
        // given: 평가 중인 안건과 별도 안건에 각각 진행자와 아이디어가 있다.
        when(participantTokenGenerator.generate()).thenReturn("eval-local-host", "eval-other-host");
        String sessionId = createSession();
        String otherSessionId = createSession();
        String foreignIdeaId = saveIdea(otherSessionId, "eval-other-host", "다른 안건 생각");
        setEvaluating(sessionId);

        // when/then: 다른 안건의 아이디어 ID는 현재 안건의 아이디어로 찾지 못해 404가 된다.
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", "eval-local-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest(foreignIdeaId, 1)))
                .andExpect(status().isNotFound());
        assertEquals(0, evaluationRepository.count());
    }

    @Test
    void rejectsValueOutsideAllowedRange() throws Exception {
        // given: 참가자가 남의 아이디어를 평가할 수 있는 단계다.
        when(participantTokenGenerator.generate()).thenReturn("eval-value-host", "eval-value-guest");
        String sessionId = createSession();
        joinSession(sessionId, "참가자");
        String ideaId = saveIdea(sessionId, "eval-value-guest", "참가자 생각");
        setEvaluating(sessionId);

        // when/then: -1·0·1 범위 밖의 값은 입력 오류로 거부된다.
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", "eval-value-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest(ideaId, 2)))
                .andExpect(status().isBadRequest());
        assertEquals(0, evaluationRepository.count());
    }

    private void clearRecords() {
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

    private void setEvaluating(String sessionId) {
        jdbcTemplate.update("UPDATE sessions SET status = ? WHERE id = ?", "EVALUATING", UUID.fromString(sessionId));
    }

    private JsonNode readItems(String sessionId, String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/sessions/{sessionId}/evaluation-items", sessionId)
                        .header("X-Participant-Token", token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void putEvaluation(String sessionId, String token, String ideaId, int value) throws Exception {
        mockMvc.perform(put("/sessions/{sessionId}/evaluations", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluationRequest(ideaId, value)))
                .andExpect(status().isOk());
    }

    private String evaluationRequest(String ideaId, int value) throws Exception {
        return objectMapper.writeValueAsString(Map.of("ideaId", ideaId, "value", value));
    }

    private Map<String, JsonNode> indexByIdeaId(JsonNode items) {
        Map<String, JsonNode> indexed = new HashMap<>();
        for (JsonNode item : items) {
            indexed.put(item.path("ideaId").asText(), item);
        }
        return indexed;
    }
}
