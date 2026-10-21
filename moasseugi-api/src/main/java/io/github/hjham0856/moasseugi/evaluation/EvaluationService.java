package io.github.hjham0856.moasseugi.evaluation;

import io.github.hjham0856.moasseugi.evaluation.EvaluationApiModels.Item;
import io.github.hjham0856.moasseugi.evaluation.EvaluationApiModels.Response;
import io.github.hjham0856.moasseugi.evaluation.EvaluationApiModels.WriteRequest;
import io.github.hjham0856.moasseugi.idea.IdeaEntity;
import io.github.hjham0856.moasseugi.idea.IdeaRepository;
import io.github.hjham0856.moasseugi.participant.CurrentParticipantService;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import io.github.hjham0856.moasseugi.session.SessionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 평가 단계의 아이디어 목록을 익명으로 제공하고 참가자의 선택을 항목별로 저장한다.
 */
@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final SessionRepository sessionRepository;
    private final IdeaRepository ideaRepository;
    private final EvaluationRepository evaluationRepository;
    private final CurrentParticipantService currentParticipantService;

    public EvaluationService(
            SessionRepository sessionRepository,
            IdeaRepository ideaRepository,
            EvaluationRepository evaluationRepository,
            CurrentParticipantService currentParticipantService
    ) {
        this.sessionRepository = sessionRepository;
        this.ideaRepository = ideaRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentParticipantService = currentParticipantService;
    }

    /**
     * EVALUATING에서 본인 아이디어를 뺀 익명 목록과 아직 선택하지 않은 상태를 포함해 반환한다.
     *
     * @throws ResponseStatusException 안건이 없으면 404, 키가 유효하지 않으면 401,
     *         평가 단계가 아니면 409
     */
    public List<Item> getItems(UUID sessionId, String participantToken) {
        ParticipantEntity participant = requireEvaluatingParticipant(sessionId, participantToken);
        List<IdeaEntity> ideas = ideaRepository.findByParticipant_Session_IdAndParticipant_IdNot(
                sessionId, participant.getId());

        if (ideas.isEmpty()) {
            return List.of();
        }

        List<UUID> ideaIds = ideas.stream().map(IdeaEntity::getId).toList();
        Map<UUID, Integer> myEvaluations = evaluationRepository
                .findByParticipant_IdAndIdea_IdIn(participant.getId(), ideaIds)
                .stream()
                .collect(Collectors.toMap(EvaluationEntity::getIdeaId, evaluation -> (int) evaluation.getValue()));

        return ideas.stream()
                .map(idea -> new Item(idea.getId(), idea.getContent(), myEvaluations.get(idea.getId())))
                .toList();
    }

    /**
     * EVALUATING에서 같은 안건의 남의 아이디어를 평가하고, 기존 선택이 있으면 값을 갱신한다.
     *
     * @throws ResponseStatusException 안건·아이디어가 없으면 404, 키가 유효하지 않으면 401,
     *         본인 아이디어이면 403, 단계가 다르면 409
     */
    @Transactional
    public Response putEvaluation(UUID sessionId, String participantToken, WriteRequest request) {
        ParticipantEntity participant = requireEvaluatingParticipant(sessionId, participantToken);

        if (ideaRepository.existsByIdAndParticipant_Id(request.ideaId(), participant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 아이디어는 평가할 수 없습니다.");
        }

        IdeaEntity idea = ideaRepository.findByIdAndParticipant_Session_Id(request.ideaId(), sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이디어를 찾을 수 없습니다."));

        var existing = evaluationRepository.findByParticipant_IdAndIdea_Id(participant.getId(), idea.getId());
        if (existing.isPresent()) {
            EvaluationEntity evaluation = existing.get();
            evaluation.updateValue(request.value().shortValue());

            return new Response(idea.getId(), evaluation.getValue());
        }

        // 같은 아이디어에 대한 첫 평가가 동시에 들어오면 둘 다 기존 평가가 없다고 보지만, DB 유일 제약이 하나만 남긴다.
        // 드문 경우라 별도 응답을 두지 않고 예상 밖 오류로 처리한다.
        EvaluationEntity evaluation = evaluationRepository.save(
                new EvaluationEntity(participant, idea, request.value().shortValue()));

        return new Response(idea.getId(), evaluation.getValue());
    }

    private ParticipantEntity requireEvaluatingParticipant(UUID sessionId, String participantToken) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다.");
        }

        ParticipantEntity participant = currentParticipantService.requireParticipant(sessionId, participantToken);
        if (participant.getSession().getStatus() != SessionStatus.EVALUATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "평가 단계에서만 아이디어를 조회하고 평가할 수 있습니다.");
        }

        return participant;
    }
}
