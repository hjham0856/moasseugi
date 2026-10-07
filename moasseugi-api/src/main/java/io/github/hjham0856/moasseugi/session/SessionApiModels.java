package io.github.hjham0856.moasseugi.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 안건 공개 정보와 생성자에게만 전달할 참가 정보를 구분하는 API 모델이다.
 */
public final class SessionApiModels {

    private SessionApiModels() {
    }

    public record CreateRequest(
            @NotNull @Size(min = 1, max = 200) String title,
            String description,
            @NotBlank @Size(max = 30) String nickname
    ) {
    }

    /**
     * 생성자가 참가자로 활동할 수 있도록 공개 안건 정보와 본인의 참가 키를 함께 반환한다.
     */
    public record CreateResponse(Detail session, ParticipantSummary participant, String participantToken) {
    }

    /**
     * 안건 생성·공개 조회에 사용하는 정보다. 참가 신원과 키는 포함하지 않는다.
     * selectedIdeaId는 채택 전에는 null이다.
     */
    public record Detail(
            UUID id,
            String title,
            String description,
            SessionStatus status,
            UUID selectedIdeaId
    ) {
    }

    /**
     * 생성·참가 응답과 진행자용 목록에 쓰는 참가 요약이다. 참가 키는 포함하지 않는다.
     */
    public record ParticipantSummary(UUID id, String nickname, boolean isHost) {
    }
}
