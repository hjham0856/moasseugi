package io.github.hjham0856.moasseugi.idea;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * 아이디어 저장·본인 조회에서 사용하는 요청과 응답이다.
 */
public final class IdeaApiModels {

    private IdeaApiModels() {
    }

    public record WriteRequest(@NotBlank String content) {
    }

    /**
     * 작성자를 공개하지 않고 아이디어 ID와 내용만 반환한다.
     */
    public record Response(UUID id, String content) {
    }
}
