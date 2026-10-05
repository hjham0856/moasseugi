package io.github.hjham0856.moasseugi.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class SessionApiModels {

    private SessionApiModels() {
    }

    public record CreateRequest(
            @NotNull @Size(min = 1, max = 200) String title,
            String description,
            @NotBlank @Size(max = 30) String nickname
    ) {
    }

    public record CreateResponse(Detail session, ParticipantSummary participant, String participantToken) {
    }

    // 안건 생성·조회 응답에서 사용하는 공개 안건 정보. 참가자 정보는 포함하지 않는다.
    public record Detail(
            UUID id,
            String title,
            String description,
            SessionStatus status,
            UUID selectedIdeaId
    ) {
    }

    // 안건 생성 응답에 담는 생성자의 참가 정보. 생성자는 진행자이면서 참가자다.
    public record ParticipantSummary(UUID id, String nickname, boolean isHost) {
    }
}
