package io.github.hjham0856.moasseugi.idea;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class IdeaApiModels {

    private IdeaApiModels() {
    }

    public record WriteRequest(@NotBlank String content) {
    }

    public record Response(UUID id, String content) {
    }
}
