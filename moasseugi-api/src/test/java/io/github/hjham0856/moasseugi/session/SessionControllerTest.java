package io.github.hjham0856.moasseugi.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @MockitoBean
    private ParticipantTokenGenerator participantTokenGenerator;

    @BeforeEach
    void clearStoredSessions() {
        participantRepository.deleteAll();
        sessionRepository.deleteAll();
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
