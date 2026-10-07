package io.github.hjham0856.moasseugi.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hjham0856.moasseugi.idea.IdeaEntity;
import io.github.hjham0856.moasseugi.idea.IdeaRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.participant.ParticipantRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 안건 생성·공개 조회와 진행자 단계 전환을 HTTP 및 저장 상태로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private IdeaRepository ideaRepository;

    @MockitoBean
    private ParticipantTokenGenerator participantTokenGenerator;

    // 테스트 사이의 기록을 분리하며 외래 키 의존 순서대로 정리한다.
    @BeforeEach
    void clearStoredSessions() {
        ideaRepository.deleteAll();
        participantRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    private String createSession(String participantToken) throws Exception {
        when(participantTokenGenerator.generate()).thenReturn(participantToken);
        MvcResult result = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"회의 안건","description":"설명","nickname":"진행자"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("session").path("id").asText();
    }

    @Test
    void createsAndReadsSession() throws Exception {
        // given: 생성자가 쓸 키를 정해 응답과 저장된 진행자 기록을 함께 확인할 수 있게 한다.
        when(participantTokenGenerator.generate()).thenReturn("host-token");

        // when: 안건을 생성하고, 참가자로 등록되지 않은 키를 보내도 안건 정보를 공개 조회한다.
        MvcResult result = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"회의 안건","description":"설명","nickname":"진행자"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode detail = objectMapper.readTree(mockMvc.perform(
                        get("/sessions/{sessionId}", response.path("session").path("id").asText())
                                .header("X-Participant-Token", "invalid-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // then: 생성된 안건과 진행자가 함께 저장되고, 조회 응답은 참가자 정보 없이 같은 안건을 반환한다.
        UUID sessionId = UUID.fromString(response.path("session").path("id").asText());
        UUID participantId = UUID.fromString(response.path("participant").path("id").asText());
        SessionEntity savedSession = sessionRepository.findById(sessionId).orElseThrow();
        ParticipantEntity savedHost = participantRepository.findById(participantId).orElseThrow();

        assertEquals(sessionId, savedHost.getSession().getId());
        assertTrue(savedHost.isHost());
        assertEquals("host-token", response.path("participantToken").asText());
        assertEquals("WRITING", response.path("session").path("status").asText());
        assertTrue(response.path("session").path("selectedIdeaId").isNull());
        assertTrue(response.path("participant").path("isHost").asBoolean());
        assertEquals("진행자", response.path("participant").path("nickname").asText());
        assertEquals(response.path("session"), detail);
        assertFalse(detail.has("viewer"));
        assertEquals("진행자", savedHost.getNickname());
        assertEquals("회의 안건", savedSession.getTitle());
    }

    @Test
    void advancesThroughEvaluationAndPublishesStoredResultStatus() throws Exception {
        // given: 진행자의 안건에 아이디어 하나를 저장하고 평가는 아직 없다.
        String sessionId = createSession("phase-host-success");
        ParticipantEntity host = participantRepository.findBySession_IdAndParticipantToken(
                UUID.fromString(sessionId), "phase-host-success").orElseThrow();
        ideaRepository.saveAndFlush(new IdeaEntity(host, "회의에서 나온 아이디어"));

        // when: 진행자가 평가를 시작한 뒤 평가가 없는 상태로 결과를 공개한다.
        String evaluationStatus = mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-host-success"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String resultStatus = mockMvc.perform(post("/sessions/{sessionId}/close-evaluation", sessionId)
                        .header("X-Participant-Token", "phase-host-success"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode publicSession = objectMapper.readTree(mockMvc.perform(
                        get("/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // then: 두 상태가 차례로 저장되고 공개 조회에도 결과 단계가 표시된다.
        assertEquals("EVALUATING", objectMapper.readTree(evaluationStatus).path("status").asText());
        assertEquals("RESULT", objectMapper.readTree(resultStatus).path("status").asText());
        assertEquals(SessionStatus.RESULT, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());
        assertEquals("RESULT", publicSession.path("status").asText());
    }

    @Test
    void refusesToStartEvaluationWithoutIdeasAndKeepsWritingStatus() throws Exception {
        // given: 아이디어가 하나도 없는 작성 단계 안건이 있다.
        String sessionId = createSession("phase-host-empty");

        // when: 진행자가 평가 시작을 요청한다.
        mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-host-empty"))
                .andExpect(status().isConflict());

        // then: 아이디어 없이 전환되지 않아 작성 단계가 유지된다.
        assertEquals(SessionStatus.WRITING, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());
    }

    @Test
    void refusesPhaseChangesFromNonHost() throws Exception {
        // given: 아이디어가 있는 안건에 진행자가 아닌 참가자가 있다.
        String sessionId = createSession("phase-host-owner");
        SessionEntity session = sessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        ParticipantEntity host = participantRepository.findBySession_IdAndParticipantToken(
                UUID.fromString(sessionId), "phase-host-owner").orElseThrow();
        participantRepository.saveAndFlush(
                new ParticipantEntity(session, "참가자", "phase-guest", false));
        ideaRepository.saveAndFlush(new IdeaEntity(host, "전환 권한 확인용 아이디어"));

        // when: 일반 참가자가 각 단계 전환을 요청한다.
        mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-guest"))
                .andExpect(status().isForbidden());
        assertEquals(SessionStatus.WRITING, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());

        mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-host-owner"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/sessions/{sessionId}/close-evaluation", sessionId)
                        .header("X-Participant-Token", "phase-guest"))
                .andExpect(status().isForbidden());

        // then: 두 경로 모두 진행자만 사용할 수 있고 거부된 요청은 현재 상태를 유지한다.
        assertEquals(SessionStatus.EVALUATING, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());
    }

    @Test
    void refusesTransitionsThatDoNotMatchCurrentPhase() throws Exception {
        // given: 진행자와 아이디어가 있는 작성 단계 안건이 있다.
        String sessionId = createSession("phase-host-order");
        ParticipantEntity host = participantRepository.findBySession_IdAndParticipantToken(
                UUID.fromString(sessionId), "phase-host-order").orElseThrow();
        ideaRepository.saveAndFlush(new IdeaEntity(host, "순서 확인용 아이디어"));

        // when/then: 평가 시작 전의 결과 전환과 이후 단계의 재전환·역전환을 거부한다.
        mockMvc.perform(post("/sessions/{sessionId}/close-evaluation", sessionId)
                        .header("X-Participant-Token", "phase-host-order"))
                .andExpect(status().isConflict());
        assertEquals(SessionStatus.WRITING, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());

        mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-host-order"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-host-order"))
                .andExpect(status().isConflict());
        assertEquals(SessionStatus.EVALUATING, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());

        mockMvc.perform(post("/sessions/{sessionId}/close-evaluation", sessionId)
                        .header("X-Participant-Token", "phase-host-order"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/sessions/{sessionId}/close-evaluation", sessionId)
                        .header("X-Participant-Token", "phase-host-order"))
                .andExpect(status().isConflict());
        assertEquals(SessionStatus.RESULT, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());

        mockMvc.perform(post("/sessions/{sessionId}/close-writing", sessionId)
                        .header("X-Participant-Token", "phase-host-order"))
                .andExpect(status().isConflict());

        // then: 허용된 순서로 도달한 결과 단계가 유지된다.
        assertEquals(SessionStatus.RESULT, sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow().getStatus());
    }

    @Test
    void rollsBackSessionWhenHostParticipantCannotBeSaved() throws Exception {
        // given: 진행자 참가자 저장이 DB의 필수 키 제약을 통과하지 못하도록 한다.
        when(participantTokenGenerator.generate()).thenReturn(null);

        // when: 안건을 생성해 진행자 저장에서 실패시킨다.
        mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"회의 안건","description":null,"nickname":"진행자"}
                                """))
                .andExpect(status().isInternalServerError());

        // then: 트랜잭션 롤백으로 안건이나 진행자만 남지 않는다.
        assertEquals(0, sessionRepository.count());
        assertEquals(0, participantRepository.count());
    }
}
