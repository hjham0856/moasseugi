package io.github.hjham0856.moasseugi.session.dto;

import io.github.hjham0856.moasseugi.session.entity.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public record SessionDetailResponse(UUID id, String title, String description, SessionStatus status, Instant writingEndsAt, Instant evaluationEndsAt, Integer participantCount, Instant serverTime, UUID selectedIdeaId, SessionViewerResponse viewer) {}
