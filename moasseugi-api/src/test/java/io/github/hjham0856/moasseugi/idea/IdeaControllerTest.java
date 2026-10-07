package io.github.hjham0856.moasseugi.idea;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.participant.ParticipantRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantTokenGenerator;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdeaControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @BeforeEach
    void clearStoredIdeas() {
        ideaRepository.deleteAll();
        participantRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @AfterEach
    void removeStoredIdeas() {
        clearStoredIdeas();
    }

    @Test
    void savesAndUpdatesOneIdeaForEachParticipant() throws Exception {
        // given: 진행자와 일반 참가자가 각자 쓸 수 있는 키를 발급받는다.
        when(participantTokenGenerator.generate()).thenReturn("idea-host", "idea-guest");
        String sessionId = createSession();
        mockMvc.perform(post("/sessions/{sessionId}/participants", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"참가자\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/sessions/{sessionId}/ideas/me", sessionId)
                        .header("X-Participant-Token", "idea-guest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("null"));

        // when: 두 사람이 저장하고, 일반 참가자가 자기 아이디어를 다시 저장한다.
        JsonNode hostIdea = putIdea(sessionId, "idea-host", "진행자 생각");
        JsonNode guestFirst = putIdea(sessionId, "idea-guest", "처음 생각");
        JsonNode guestUpdated = putIdea(sessionId, "idea-guest", "고친 생각");
        JsonNode hostRead = readMyIdea(sessionId, "idea-host");
        JsonNode guestRead = readMyIdea(sessionId, "idea-guest");

        // then: 저장 전 응답은 JSON null이고 수정은 같은 ID만 갱신하며 서로의 기록은 노출되지 않는다.
        assertEquals(2, ideaRepository.count());
        assertNotEquals(hostIdea.path("id").asText(), guestFirst.path("id").asText());
        assertEquals(guestFirst.path("id").asText(), guestUpdated.path("id").asText());
        assertEquals("진행자 생각", hostRead.path("content").asText());
        assertEquals("고친 생각", guestRead.path("content").asText());
        assertEquals(hostIdea.path("id").asText(), hostRead.path("id").asText());
    }

    @Test
    void rejectsBlankContent() throws Exception {
        // given: 작성 가능한 참가자 키가 있다.
        when(participantTokenGenerator.generate()).thenReturn("idea-blank-host");
        String sessionId = createSession();

        // when: 공백만 있는 아이디어를 저장한다.
        mockMvc.perform(put("/sessions/{sessionId}/ideas", sessionId)
                        .header("X-Participant-Token", "idea-blank-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest());

        // then: 잘못된 내용은 저장되지 않는다.
        assertEquals(0, ideaRepository.count());
    }

    @Test
    void rejectsMissingInvalidOrOtherSessionsToken() throws Exception {
        // given: 두 안건에 서로 다른 참가 키가 있다.
        when(participantTokenGenerator.generate()).thenReturn("idea-host-one", "idea-host-two");
        String sessionId = createSession();
        createSession();

        // when/then: 누락·잘못된 키와 다른 안건의 키는 모두 현재 안건 참가자로 인정하지 않는다.
        mockMvc.perform(get("/sessions/{sessionId}/ideas/me", sessionId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/sessions/{sessionId}/ideas/me", sessionId)
                        .header("X-Participant-Token", "invalid-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/sessions/{sessionId}/ideas/me", sessionId)
                        .header("X-Participant-Token", "idea-host-two"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsNotFoundForMissingSession() throws Exception {
        // given: 유효한 안건과 진행자 키가 있고, 별도의 안건 ID는 존재하지 않는다.
        when(participantTokenGenerator.generate()).thenReturn("idea-not-found-host");
        String sessionId = createSession();

        // when: 없는 안건의 아이디어를 조회한다.
        mockMvc.perform(get("/sessions/{sessionId}/ideas/me", UUID.randomUUID())
                        .header("X-Participant-Token", "idea-not-found-host"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsSavingAfterWriting() throws Exception {
        // given: 진행자가 작성 중 아이디어를 저장한 뒤 안건을 평가 단계로 전환했다.
        when(participantTokenGenerator.generate()).thenReturn("idea-stage-host");
        String sessionId = createSession();
        JsonNode saved = putIdea(sessionId, "idea-stage-host", "처음 생각");
        jdbcTemplate.update("UPDATE sessions SET status = ? WHERE id = ?", "EVALUATING", UUID.fromString(sessionId));

        // when/then: 평가 단계에서는 기존 아이디어 수정도 409로 거부되고 저장 내용이 유지된다.
        mockMvc.perform(put("/sessions/{sessionId}/ideas", sessionId)
                        .header("X-Participant-Token", "idea-stage-host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"늦은 생각\"}"))
                .andExpect(status().isConflict());
        assertEquals(1, ideaRepository.count());
        assertEquals("처음 생각", ideaRepository.findById(UUID.fromString(saved.path("id").asText()))
                .orElseThrow().getContent());
    }

    @Test
    void databaseRejectsSecondIdeaForSameParticipantWithHandledConstraintName() throws Exception {
        // given: 참가자의 첫 아이디어가 DB에 저장되어 있다.
        when(participantTokenGenerator.generate()).thenReturn("idea-unique-host");
        String sessionId = createSession();
        putIdea(sessionId, "idea-unique-host", "첫 아이디어");
        ParticipantEntity participant = participantRepository.findBySession_IdAndParticipantToken(
                UUID.fromString(sessionId), "idea-unique-host").orElseThrow();

        // when: 같은 참가자 ID로 두 번째 행을 직접 저장한다.
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> ideaRepository.saveAndFlush(new IdeaEntity(participant, "중복 아이디어")));

        // then: DB가 중복을 거부하고, Hibernate가 서비스의 409 분기에서 찾는 제약 이름을 제공한다.
        Throwable cause = exception;
        ConstraintViolationException violation = null;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException found) {
                violation = found;
                break;
            }
            cause = cause.getCause();
        }
        assertNotNull(violation);
        assertTrue(violation.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_idea_participant"));
        assertEquals(1, ideaRepository.count());
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

    private JsonNode putIdea(String sessionId, String token, String content) throws Exception {
        MvcResult result = mockMvc.perform(put("/sessions/{sessionId}/ideas", sessionId)
                        .header("X-Participant-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", content))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode readMyIdea(String sessionId, String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/sessions/{sessionId}/ideas/me", sessionId)
                        .header("X-Participant-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
