package io.github.hjham0856.moasseugi.evaluation.controller;

import io.github.hjham0856.moasseugi.evaluation.dto.EvaluationItemResponse;
import io.github.hjham0856.moasseugi.evaluation.dto.EvaluationResponse;
import io.github.hjham0856.moasseugi.evaluation.dto.SaveEvaluationRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

@Controller
public abstract class EvaluationController {
    @GetMapping("/api/sessions/{sessionId}/evaluation-items")
    public abstract List<EvaluationItemResponse> getEvaluationItems(UUID sessionId);

    @PutMapping("/api/sessions/{sessionId}/evaluations")
    public abstract EvaluationResponse putEvaluation(UUID sessionId, SaveEvaluationRequest request);

    // TODO: 참가자 인증, 본인 아이디어 제외, 평가 단계 및 Upsert 구현
}
