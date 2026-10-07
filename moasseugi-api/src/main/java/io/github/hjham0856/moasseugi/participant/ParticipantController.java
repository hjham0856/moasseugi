package io.github.hjham0856.moasseugi.participant;

import io.github.hjham0856.moasseugi.participant.ParticipantApiModels.JoinRequest;
import io.github.hjham0856.moasseugi.participant.ParticipantApiModels.JoinResponse;
import io.github.hjham0856.moasseugi.session.SessionApiModels;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 신규 참가와 진행자 전용 참가자 목록 API를 제공한다.
 */
@RestController
@RequestMapping("/sessions/{sessionId}/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JoinResponse joinSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody JoinRequest request
    ) {
        return participantService.joinSession(sessionId, request);
    }

    @GetMapping
    public List<SessionApiModels.ParticipantSummary> getParticipants(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken
    ) {
        return participantService.getParticipants(sessionId, participantToken);
    }
}
