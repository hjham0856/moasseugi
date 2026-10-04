package io.github.hjham0856.moasseugi.participant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CurrentParticipantService {

    private final ParticipantRepository participantRepository;

    public CurrentParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    public ParticipantEntity requireParticipant(UUID sessionId, String participantToken) {
        if (participantToken == null || participantToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "참가자 키가 필요합니다.");
        }

        return participantRepository.findBySession_IdAndReconnectToken(sessionId, participantToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "유효하지 않은 참가자 키입니다."));
    }

    public ParticipantEntity requireHost(UUID sessionId, String participantToken) {
        ParticipantEntity participant = requireParticipant(sessionId, participantToken);
        if (!participant.isHost()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "진행자만 수행할 수 있습니다.");
        }
        return participant;
    }
}
