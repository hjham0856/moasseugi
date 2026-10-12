package io.github.hjham0856.moasseugi.evaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 익명 평가 대상 조회와 개별 평가 저장에 사용하는 요청과 응답이다.
 */
public final class EvaluationApiModels {

    private EvaluationApiModels() {
    }

    public record WriteRequest(
            @NotNull UUID ideaId,
            @NotNull @Min(-1) @Max(1) Integer value
    ) {
    }

    /**
     * 작성자 정보 없이 아이디어와 현재 참가자의 평가만 반환한다.
     */
    public record Item(UUID ideaId, String content, Integer myEvaluation) {
    }

    public record Response(UUID ideaId, int value) {
    }
}
