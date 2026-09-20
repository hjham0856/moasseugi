package io.github.hjham0856.moasseugi.idea.dto;

import java.time.Instant;
import java.util.UUID;

public record IdeaResponse(UUID id, String content, Instant createdAt, Instant updatedAt) {}
