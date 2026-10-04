package io.github.hjham0856.moasseugi.participant;

import io.github.hjham0856.moasseugi.session.SessionEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CurrentParticipantServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ParticipantRepository participantRepository;

    @Test
    void findsParticipantsBySessionAndTokenAndDistinguishesHostRole() {
        // given: 같은 안건의 진행자와 일반 참가자를 각각의 키로 저장해 역할을 비교할 수 있게 한다.
        SessionEntity session = new SessionEntity("회의", null);
        entityManager.persist(session);
        ParticipantEntity host = new ParticipantEntity(session, "진행자", "host-token", true);
        ParticipantEntity participant = new ParticipantEntity(session, "참가자", "participant-token", false);
        entityManager.persist(host);
        entityManager.persist(participant);
        entityManager.flush();

        CurrentParticipantService service = new CurrentParticipantService(participantRepository);

        // when: 두 참가자의 키로 식별하고 진행자 전용 확인을 요청한다.
        ParticipantEntity foundHost = service.requireParticipant(session.getId(), "host-token");
        ParticipantEntity foundParticipant = service.requireParticipant(session.getId(), "participant-token");
        ParticipantEntity authorizedHost = service.requireHost(session.getId(), "host-token");

        // then: 각 키는 해당 참가자를 찾고 진행자 여부가 구분된다.
        assertSame(host, foundHost);
        assertSame(participant, foundParticipant);
        assertTrue(foundHost.isHost());
        assertFalse(foundParticipant.isHost());
        assertSame(host, authorizedHost);
    }

    @Test
    void rejectsInvalidToken() {
        // given: 안건은 존재하지만 등록된 참가 기록은 없다.
        SessionEntity session = new SessionEntity("회의", null);
        entityManager.persist(session);
        entityManager.flush();

        CurrentParticipantService service = new CurrentParticipantService(participantRepository);

        // when: 등록되지 않은 키로 해당 안건의 참가자를 찾는다.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requireParticipant(session.getId(), "invalid-token"));

        // then: 유효하지 않은 키는 인증되지 않는다.
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void rejectsTokenBelongingToAnotherSession() {
        // given: 참가 키는 다른 안건의 참가 기록에만 등록되어 있다.
        SessionEntity requestedSession = new SessionEntity("회의 1", null);
        SessionEntity tokenSession = new SessionEntity("회의 2", null);
        entityManager.persist(requestedSession);
        entityManager.persist(tokenSession);
        entityManager.persist(new ParticipantEntity(tokenSession, "참가자", "other-session-token", false));
        entityManager.flush();

        CurrentParticipantService service = new CurrentParticipantService(participantRepository);

        // when: 키가 등록된 안건과 다른 안건에서 참가자를 찾는다.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requireParticipant(requestedSession.getId(), "other-session-token"));

        // then: 다른 안건의 키는 이 안건의 참가자로 인정되지 않는다.
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void rejectsRegularParticipantFromHostOnlyOperation() {
        // given: 해당 안건에 일반 참가자의 기록과 키가 저장되어 있다.
        SessionEntity session = new SessionEntity("회의", null);
        entityManager.persist(session);
        entityManager.persist(new ParticipantEntity(session, "참가자", "participant-token", false));
        entityManager.flush();

        CurrentParticipantService service = new CurrentParticipantService(participantRepository);

        // when: 일반 참가자 키로 진행자 전용 확인을 요청한다.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requireHost(session.getId(), "participant-token"));

        // then: 일반 참가자는 진행자로 인정되지 않는다.
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}
