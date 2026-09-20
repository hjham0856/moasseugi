package io.github.hjham0856.moasseugi.evaluation.dto;

import java.util.UUID;

public record EvaluationItemResponse(UUID ideaId, String content, Integer myEvaluation) {}
