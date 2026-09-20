package io.github.hjham0856.moasseugi.participant.dto;

public record JoinSessionResponse(ParticipantSummaryResponse participant, String reconnectToken) {}
