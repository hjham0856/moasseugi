package io.github.hjham0856.moasseugi.user.dto;

public record LoginResponse(String accessToken, String tokenType, UserResponse user) {}
