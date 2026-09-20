package io.github.hjham0856.moasseugi.session.dto;

import java.util.UUID;

public record SessionViewerResponse(Boolean isHost, Boolean isParticipant, UUID participantId, String nickname) {}
