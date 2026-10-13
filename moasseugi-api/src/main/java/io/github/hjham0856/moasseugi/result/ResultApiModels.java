package io.github.hjham0856.moasseugi.result;

import io.github.hjham0856.moasseugi.session.SessionStatus;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 결과 조회와 아이디어 채택에 사용하는 요청과 응답이다.
 */
public final class ResultApiModels {

    private ResultApiModels() {
    }

    public record SelectRequest(@NotNull UUID ideaId) {
    }

    public record Response(UUID sessionId, SessionStatus status, UUID selectedIdeaId, List<IdeaResult> ideas) {
    }

    /**
     * 작성자 닉네임은 채택된 아이디어에만 담기고 나머지는 null이다.
     */
    public record IdeaResult(
            UUID ideaId,
            String content,
            String authorNickname,
            Distribution distribution,
            boolean selected
    ) {
    }

    /**
     * 저장된 평가의 개수이며 total은 세 값의 합이다. 점수나 순위는 계산하지 않는다.
     */
    public record Distribution(int positive, int neutral, int negative, int total) {
    }
}
