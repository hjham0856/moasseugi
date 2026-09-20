package io.github.hjham0856.moasseugi.participant.dto;

import java.util.UUID;

public record ParticipantSummaryResponse(UUID id, String nickname, Boolean isHost) {}
