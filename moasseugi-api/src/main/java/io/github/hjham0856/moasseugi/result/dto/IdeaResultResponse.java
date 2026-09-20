package io.github.hjham0856.moasseugi.result.dto;

import java.util.UUID;

public record IdeaResultResponse(UUID ideaId, String content, String authorNickname, EvaluationDistributionResponse distribution, Boolean selected) {}
