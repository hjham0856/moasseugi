package io.github.hjham0856.moasseugi.participant;

import io.github.hjham0856.moasseugi.session.SessionApiModels;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ParticipantApiModels {

    private ParticipantApiModels() {
    }

    public record JoinRequest(
            @NotBlank @Size(max = 30) String nickname
    ) {
    }

    // 신규 참가 응답. 참가자 식별에 필요한 최소 정보와 안건별 참가 키만 반환한다.
    public record JoinResponse(
            SessionApiModels.ParticipantSummary participant,
            String participantToken
    ) {
    }
}
