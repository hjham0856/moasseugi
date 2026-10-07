package io.github.hjham0856.moasseugi.participant;

import io.github.hjham0856.moasseugi.session.SessionApiModels;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 계정 없이 닉네임으로 참가할 때 사용하는 요청과 응답이다.
 */
public final class ParticipantApiModels {

    private ParticipantApiModels() {
    }

    public record JoinRequest(
            @NotBlank @Size(max = 30) String nickname
    ) {
    }

    /**
     * 신규 참가자 본인에게 이후 신원 확인에 필요한 안건별 참가 키를 전달한다.
     */
    public record JoinResponse(
            SessionApiModels.ParticipantSummary participant,
            String participantToken
    ) {
    }
}
