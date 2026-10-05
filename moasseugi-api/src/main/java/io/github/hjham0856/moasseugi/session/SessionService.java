package io.github.hjham0856.moasseugi.session;

import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.participant.ParticipantRepository;
import io.github.hjham0856.moasseugi.participant.ParticipantService;
import io.github.hjham0856.moasseugi.participant.ParticipantTokenGenerator;
import io.github.hjham0856.moasseugi.session.SessionApiModels.CreateRequest;
import io.github.hjham0856.moasseugi.session.SessionApiModels.CreateResponse;
import io.github.hjham0856.moasseugi.session.SessionApiModels.Detail;
import io.github.hjham0856.moasseugi.session.SessionApiModels.ParticipantSummary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantTokenGenerator participantTokenGenerator;

    public SessionService(
            SessionRepository sessionRepository,
            ParticipantRepository participantRepository,
            ParticipantTokenGenerator participantTokenGenerator
    ) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.participantTokenGenerator = participantTokenGenerator;
    }

    @Transactional
    public CreateResponse createSession(CreateRequest request) {
        // 진행자 닉네임도 신규 참가와 같은 확정 정책(앞뒤 공백 제거·대소문자 구분)으로 정규화한다.
        String nickname = ParticipantService.normalizeNickname(request.nickname());
        SessionEntity session = sessionRepository.save(
                new SessionEntity(request.title(), request.description()));
        String participantToken = participantTokenGenerator.generate();
        ParticipantEntity host = participantRepository.save(
                new ParticipantEntity(session, nickname, participantToken, true));

        return new CreateResponse(toDetail(session), toSummary(host), participantToken);
    }

    public Detail getSession(UUID sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다."));
        return toDetail(session);
    }

    private Detail toDetail(SessionEntity session) {
        return new Detail(
                session.getId(),
                session.getTitle(),
                session.getDescription(),
                session.getStatus(),
                session.getSelectedIdeaId()
        );
    }

    private ParticipantSummary toSummary(ParticipantEntity participant) {
        return new ParticipantSummary(participant.getId(), participant.getNickname(), participant.isHost());
    }
}
