package io.github.hjham0856.moasseugi.participant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * 안건별 참가 키로 신원과 진행자 권한을 확인하는 단일 진입점이다.
 */
@Service
@Transactional(readOnly = true)
public class CurrentParticipantService {

    private final ParticipantRepository participantRepository;

    public CurrentParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    /**
     * 해당 안건에 발급된 키의 참가자를 반환한다. 안건 단계와 관계없이 기존 키를 인정한다.
     * 안건 자체의 존재 여부는 호출자가 별도로 확인한다.
     *
     * @throws ResponseStatusException 키가 없거나 해당 안건의 참가자를 찾지 못하면 401
     */
    public ParticipantEntity requireParticipant(UUID sessionId, String participantToken) {
        if (participantToken == null || participantToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "참가자 키가 필요합니다.");
        }

        return participantRepository.findBySession_IdAndParticipantToken(sessionId, participantToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "유효하지 않은 참가자 키입니다."));
    }

    /**
     * 참가 신원을 확인한 뒤 진행자에게만 접근을 허용한다.
     *
     * @throws ResponseStatusException 키가 유효하지 않으면 401, 일반 참가자이면 403
     */
    public ParticipantEntity requireHost(UUID sessionId, String participantToken) {
        ParticipantEntity participant = requireParticipant(sessionId, participantToken);

        if (!participant.isHost()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "진행자만 수행할 수 있습니다.");
        }

        return participant;
    }
}
