package io.github.hjham0856.moasseugi.result;

import io.github.hjham0856.moasseugi.evaluation.EvaluationEntity;
import io.github.hjham0856.moasseugi.evaluation.EvaluationRepository;
import io.github.hjham0856.moasseugi.idea.IdeaEntity;
import io.github.hjham0856.moasseugi.idea.IdeaRepository;
import io.github.hjham0856.moasseugi.participant.CurrentParticipantService;
import io.github.hjham0856.moasseugi.result.ResultApiModels.Distribution;
import io.github.hjham0856.moasseugi.result.ResultApiModels.IdeaResult;
import io.github.hjham0856.moasseugi.result.ResultApiModels.Response;
import io.github.hjham0856.moasseugi.result.ResultApiModels.SelectRequest;
import io.github.hjham0856.moasseugi.session.SessionEntity;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import io.github.hjham0856.moasseugi.session.SessionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RESULT 단계의 평가 분포를 집계하고 진행자의 아이디어 채택을 처리한다.
 * 결과는 점수나 순위 없이 분포만 보여주며, 채택 여부는 채택 아이디어 값의 존재로 표현한다.
 */
@Service
@Transactional(readOnly = true)
public class ResultService {

    private final SessionRepository sessionRepository;
    private final IdeaRepository ideaRepository;
    private final EvaluationRepository evaluationRepository;
    private final CurrentParticipantService currentParticipantService;

    public ResultService(
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
     * 참가 키 없이 링크로 결과를 공개 조회한다. 아이디어는 생성순이며 평가가 없는 아이디어도 포함한다.
     * 채택 전에는 모든 작성자가 숨겨지고, 채택 후에는 채택작의 작성자 닉네임만 공개한다.
     *
     * @throws ResponseStatusException 안건이 없으면 404, 현재 단계가 RESULT가 아니면 409
     */
    public Response getResults(UUID sessionId) {
        SessionEntity session = requireSession(sessionId);
        requireResultStatus(session);

        return toResponse(session);
    }

    /**
     * 진행자가 RESULT에서 같은 안건의 아이디어 하나를 채택한다. 평가가 없어도 채택할 수 있다.
     * 채택 후에도 단계는 RESULT이며 변경할 수 없다.
     *
     * @throws ResponseStatusException 안건이 없거나 이 안건의 아이디어가 아니면 404,
     *         키가 유효하지 않으면 401, 진행자가 아니면 403, 단계가 RESULT가 아니거나 이미 채택했으면 409
     */
    @Transactional
    public Response selectIdea(UUID sessionId, String participantToken, SelectRequest request) {
        SessionEntity session = requireSession(sessionId);
        currentParticipantService.requireHost(sessionId, participantToken);
        requireResultStatus(session);

        if (session.getSelectedIdea() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 채택한 아이디어가 있습니다.");
        }

        IdeaEntity idea = ideaRepository.findByIdAndParticipant_Session_Id(request.ideaId(), sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이디어를 찾을 수 없습니다."));

        session.selectIdea(idea);
        return toResponse(session);
    }

    private SessionEntity requireSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다."));
    }

    private void requireResultStatus(SessionEntity session) {
        if (session.getStatus() != SessionStatus.RESULT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결과 단계에서만 수행할 수 있습니다.");
        }
    }

    private Response toResponse(SessionEntity session) {
        UUID sessionId = session.getId();
        UUID selectedIdeaId = session.getSelectedIdeaId();

        Map<UUID, int[]> countsByIdea = countByIdea(sessionId);
        List<IdeaResult> ideas = ideaRepository.findByParticipant_Session_IdOrderByCreatedAtAscIdAsc(sessionId)
                .stream()
                .map(idea -> toIdeaResult(idea, countsByIdea.get(idea.getId()), selectedIdeaId))
                .toList();

        return new Response(sessionId, session.getStatus(), selectedIdeaId, ideas);
    }

    // 값별 개수를 [좋음, 괜찮음, 별로] 순서로 센다. 평가가 없는 아이디어는 맵에 없다.
    private Map<UUID, int[]> countByIdea(UUID sessionId) {
        Map<UUID, int[]> counts = new HashMap<>();

        for (EvaluationEntity evaluation : evaluationRepository.findByIdea_Participant_Session_Id(sessionId)) {
            int[] count = counts.computeIfAbsent(evaluation.getIdeaId(), id -> new int[3]);

            switch (evaluation.getValue()) {
                case 1 -> count[0]++;
                case 0 -> count[1]++;
                default -> count[2]++;
            }
        }

        return counts;
    }

    private IdeaResult toIdeaResult(IdeaEntity idea, int[] counts, UUID selectedIdeaId) {
        int[] c = counts == null ? new int[3] : counts;
        boolean selected = idea.getId().equals(selectedIdeaId);

        return new IdeaResult(
                idea.getId(),
                idea.getContent(),
                selected ? idea.getParticipant().getNickname() : null,
                new Distribution(c[0], c[1], c[2], c[0] + c[1] + c[2]),
                selected
        );
    }
}
