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

/**
 * 안건과 생성자의 진행자 참가 기록을 함께 만들고, 안건 공개 정보를 조회한다.
 */
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

    /**
     * 작성 단계의 안건과 진행자를 하나의 트랜잭션으로 생성하고 참가 키를 반환한다.
     * 진행자 저장에 실패하면 안건도 남기지 않는다.
     *
     * @throws ResponseStatusException 닉네임이 없거나 정규화 후 30자를 넘으면 400
     */
    @Transactional
    public CreateResponse createSession(CreateRequest request) {
        // 생성자와 신규 참가자에게 같은 닉네임 규칙을 적용한다.
        String nickname = ParticipantService.normalizeNickname(request.nickname());

        SessionEntity session = sessionRepository.save(
                new SessionEntity(request.title(), request.description()));

        String participantToken = participantTokenGenerator.generate();
        ParticipantEntity host = participantRepository.save(
                new ParticipantEntity(session, nickname, participantToken, true));

        return new CreateResponse(toDetail(session), toSummary(host), participantToken);
    }

    /**
     * 참가 여부와 관계없이 안건 공개 정보를 반환한다. 참가 신원이나 키는 포함하지 않는다.
     *
     * @throws ResponseStatusException 안건이 없으면 404
     */
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
