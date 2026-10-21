package io.github.hjham0856.moasseugi.idea;

import io.github.hjham0856.moasseugi.idea.IdeaApiModels.Response;
import io.github.hjham0856.moasseugi.idea.IdeaApiModels.WriteRequest;
import io.github.hjham0856.moasseugi.participant.CurrentParticipantService;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import io.github.hjham0856.moasseugi.session.SessionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * 참가자별 하나의 아이디어를 명시적으로 저장·수정하고, 본인 기록만 반환한다.
 */
@Service
@Transactional(readOnly = true)
public class IdeaService {

    private final SessionRepository sessionRepository;
    private final IdeaRepository ideaRepository;
    private final CurrentParticipantService currentParticipantService;

    public IdeaService(
            SessionRepository sessionRepository,
            IdeaRepository ideaRepository,
            CurrentParticipantService currentParticipantService
    ) {
        this.sessionRepository = sessionRepository;
        this.ideaRepository = ideaRepository;
        this.currentParticipantService = currentParticipantService;
    }

    /**
     * 작성 중 본인의 아이디어를 저장한다. 이미 있으면 같은 ID의 내용을 수정한다.
     * 요청 내용의 공백 여부는 API 입력 검증에서 확인하며, 본문은 그대로 저장한다.
     *
     * @throws ResponseStatusException 안건이 없으면 404, 키가 유효하지 않으면 401,
     *         작성 종료이면 409
     */
    @Transactional
    public Response putIdea(UUID sessionId, String participantToken, WriteRequest request) {
        ParticipantEntity participant = requireParticipant(sessionId, participantToken);
        requireWriting(participant);

        Optional<IdeaEntity> existing = ideaRepository.findByParticipant_Id(participant.getId());
        if (existing.isPresent()) {
            IdeaEntity idea = existing.get();
            idea.updateContent(request.content());

            return toResponse(idea);
        }

        // 같은 참가자의 첫 저장이 동시에 들어오면 둘 다 기존 아이디어가 없다고 보지만, DB 유일 제약이 하나만 남긴다.
        // 드문 경우라 별도 응답을 두지 않고 예상 밖 오류로 처리한다.
        return toResponse(ideaRepository.save(new IdeaEntity(participant, request.content())));
    }

    /**
     * 단계와 관계없이 본인의 아이디어만 반환하며, 아직 저장하지 않았다면 빈 Optional을 반환한다.
     *
     * @throws ResponseStatusException 안건이 없으면 404, 키가 유효하지 않으면 401
     */
    public Optional<Response> getMyIdea(UUID sessionId, String participantToken) {
        ParticipantEntity participant = requireParticipant(sessionId, participantToken);

        return ideaRepository.findByParticipant_Id(participant.getId()).map(this::toResponse);
    }

    private ParticipantEntity requireParticipant(UUID sessionId, String participantToken) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다.");
        }

        return currentParticipantService.requireParticipant(sessionId, participantToken);
    }

    private void requireWriting(ParticipantEntity participant) {
        if (participant.getSession().getStatus() != SessionStatus.WRITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "작성 단계에서만 아이디어를 저장할 수 있습니다.");
        }
    }

    private Response toResponse(IdeaEntity idea) {
        return new Response(idea.getId(), idea.getContent());
    }
}
