package io.github.hjham0856.moasseugi.result.dto;

import io.github.hjham0856.moasseugi.session.entity.SessionStatus;
import java.util.List;
import java.util.UUID;

public record ResultResponse(UUID sessionId, SessionStatus status, List<IdeaResultResponse> ideas) {}
