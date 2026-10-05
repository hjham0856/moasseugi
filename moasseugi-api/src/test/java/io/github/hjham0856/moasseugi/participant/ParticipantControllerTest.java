package io.github.hjham0856.moasseugi.participant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hjham0856.moasseugi.session.SessionRepository;
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

import java.util.HashSet;
import java.util.Set;
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
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentParticipantService currentParticipantService;

    @MockitoBean
    private ParticipantTokenGenerator participantTokenGenerator;

    @BeforeEach
    void clearStoredSessions() {
        participantRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void joinsAndListsParticipantsWithSessionSeparation() throws Exception {
        // given: 진행자 ' Alex '는 'Alex'로 다듬어진다
        when(participantTokenGenerator.generate()).thenReturn("host-token", "join-token", "other-host", "other-join");

        MvcResult created = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\" Alex \"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdResponse = objectMapper.readTree(created.getResponse().getContentAsString());
        String sessionId = createdResponse.path("session").path("id").asText();

        // when: 대소문자만 다른 ' alex '로 참가한다
        MvcResult joined = mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" alex \"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode joinResponse = objectMapper.readTree(joined.getResponse().getContentAsString());
        MvcResult listed = mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId)
                        .header("X-Participant-Token", "host-token"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode participants = objectMapper.readTree(listed.getResponse().getContentAsString());

        MvcResult otherCreated = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"다른 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String otherSessionId = objectMapper.readTree(otherCreated.getResponse().getContentAsString())
                .path("session").path("id").asText();

        // then: 발급 키로 같은 참가자를 찾고 목록은 최소 정보만 공개한다
        assertEquals("Alex", createdResponse.path("participant").path("nickname").asText());
        assertEquals("alex", joinResponse.path("participant").path("nickname").asText());
        assertFalse(joinResponse.path("participant").path("isHost").asBoolean());
        assertEquals("join-token", joinResponse.path("participantToken").asText());
        assertTrue(participantRepository.existsBySession_IdAndNickname(UUID.fromString(sessionId), "alex"));
        ParticipantEntity foundByToken = currentParticipantService.requireParticipant(
                UUID.fromString(sessionId), "join-token");
        assertEquals(UUID.fromString(joinResponse.path("participant").path("id").asText()),
                foundByToken.getId());
        assertEquals(UUID.fromString(sessionId), foundByToken.getSession().getId());

        assertEquals(2, participants.size());
        Set<String> nicknames = new HashSet<>();
        for (JsonNode participant : participants) {
            assertTrue(participant.has("id"));
            assertTrue(participant.has("nickname"));
            assertTrue(participant.has("isHost"));
            assertFalse(participant.has("participantToken"));
            assertEquals(3, participant.size());
            nicknames.add(participant.path("nickname").asText());
        }
        assertEquals(Set.of("Alex", "alex"), nicknames);

        mockMvc.perform(post("/sessions/{sessionId}/participants", otherSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"alex\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsDuplicateNicknameIncludingHost() throws Exception {
        // given: 진행자와 참가자가 이미 있다
        when(participantTokenGenerator.generate()).thenReturn("host-token", "join-token");
        MvcResult created = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("session").path("id").asText();
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"참가자\"}"))
                .andExpect(status().isCreated());

        // when: 공백이 달라도 같은 닉네임으로 참가한다
        // then: 진행자 닉네임까지 같은 안건 중복은 거부되고 기록은 늘지 않는다
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" 참가자 \"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" 진행자 \"}"))
                .andExpect(status().isConflict());
        assertEquals(2, participantRepository.count());
    }

    @Test
    void rejectsNewJoinAfterWritingButKeepsExistingKey() throws Exception {
        // given: 작성 중에 발급된 참가자 키가 있다
        when(participantTokenGenerator.generate()).thenReturn("host-token", "join-token");
        MvcResult created = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("session").path("id").asText();
        MvcResult joined = mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"참가자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String participantId = objectMapper.readTree(joined.getResponse().getContentAsString())
                .path("participant").path("id").asText();

        // when: 단계를 바꾼 뒤 신규 참가와 기존 키 식별을 요청한다
        // then: 신규 참가는 거부되고 기존 키 식별은 유지되며 기록은 늘지 않는다
        jdbcTemplate.update("UPDATE sessions SET status = ? WHERE id = ?", "EVALUATING",
                UUID.fromString(sessionId));
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새참가자\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId)
                        .header("X-Participant-Token", "host-token"))
                .andExpect(status().isOk());
        assertEquals(UUID.fromString(participantId), currentParticipantService
                .requireParticipant(UUID.fromString(sessionId), "join-token").getId());
        assertEquals(2, participantRepository.count());

        jdbcTemplate.update("UPDATE sessions SET status = ? WHERE id = ?", "RESULT",
                UUID.fromString(sessionId));
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"또다른참가자\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId)
                        .header("X-Participant-Token", "host-token"))
                .andExpect(status().isOk());
        assertEquals(UUID.fromString(participantId), currentParticipantService
                .requireParticipant(UUID.fromString(sessionId), "join-token").getId());
        assertEquals(2, participantRepository.count());
    }

    @Test
    void returnsNotFoundForMissingSession() throws Exception {
        // given: 없는 안건 ID
        String missingSessionId = UUID.randomUUID().toString();

        // when: 없는 안건에 참가하거나 목록을 조회한다
        // then: 안건 없음을 반환한다
        mockMvc.perform(post("/sessions/{sessionId}/participants", missingSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"참가자\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/sessions/{sessionId}/participants", missingSessionId)
                        .header("X-Participant-Token", "some-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankNickname() throws Exception {
        // given: 작성 중인 안건
        when(participantTokenGenerator.generate()).thenReturn("host-token");
        MvcResult created = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("session").path("id").asText();

        // when: 공백뿐인 닉네임으로 참가한다
        // then: 입력 오류로 거부된다
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingInvalidOrForeignToken() throws Exception {
        // given: 두 안건에 발급된 키가 있다
        when(participantTokenGenerator.generate()).thenReturn("host-token", "other-host-token");
        MvcResult created = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("session").path("id").asText();
        mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"다른 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated());

        // when: 키 없이·잘못된 키·다른 안건 키로 조회한다
        // then: 모두 인증되지 않는다
        mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId)
                        .header("X-Participant-Token", "invalid-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId)
                        .header("X-Participant-Token", "other-host-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRegularParticipantFromListing() throws Exception {
        // given: 일반 참가자 키가 있다
        when(participantTokenGenerator.generate()).thenReturn("host-token", "join-token");
        MvcResult created = mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"회의 안건\",\"description\":null,\"nickname\":\"진행자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("session").path("id").asText();
        MvcResult joined = mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"참가자\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String participantToken = objectMapper.readTree(joined.getResponse().getContentAsString())
                .path("participantToken").asText();

        // when: 진행자 전용 목록을 조회한다
        // then: 진행자가 아니므로 거부된다
        mockMvc.perform(get("/sessions/{sessionId}/participants", sessionId)
                        .header("X-Participant-Token", participantToken))
                .andExpect(status().isForbidden());
    }
}
