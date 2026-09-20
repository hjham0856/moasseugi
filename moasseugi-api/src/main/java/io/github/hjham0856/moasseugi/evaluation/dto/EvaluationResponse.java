package io.github.hjham0856.moasseugi.evaluation.dto;

import java.time.Instant;
import java.util.UUID;

public record EvaluationResponse(UUID ideaId, Integer value, Instant updatedAt) {}
