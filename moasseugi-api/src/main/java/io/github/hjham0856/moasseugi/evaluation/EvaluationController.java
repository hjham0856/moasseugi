package io.github.hjham0856.moasseugi.evaluation;

import io.github.hjham0856.moasseugi.evaluation.EvaluationApiModels.Item;
import io.github.hjham0856.moasseugi.evaluation.EvaluationApiModels.Response;
import io.github.hjham0856.moasseugi.evaluation.EvaluationApiModels.WriteRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 익명 평가 대상 조회와 클릭 단위 평가 저장 API를 제공한다.
 */
@RestController
@RequestMapping("/sessions/{sessionId}")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/evaluation-items")
    public List<Item> getItems(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken
    ) {
        return evaluationService.getItems(sessionId, participantToken);
    }

    @PutMapping("/evaluations")
    public Response putEvaluation(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken,
            @Valid @RequestBody WriteRequest request
    ) {
        return evaluationService.putEvaluation(sessionId, participantToken, request);
    }
}
