package io.github.hjham0856.moasseugi.session.dto;

import java.time.Instant;

public record CreateSessionRequest(String title, String description, Instant writingEndsAt, Instant evaluationEndsAt) {}
