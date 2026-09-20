package io.github.hjham0856.moasseugi.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, Instant createdAt) {}
