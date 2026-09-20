package io.github.hjham0856.moasseugi.session.dto;

import java.time.Instant;

public record UpdateSessionRequest(String title, String description, Instant writingEndsAt, Instant evaluationEndsAt) {}
