package io.github.hjham0856.moasseugi.participant.controller;

import io.github.hjham0856.moasseugi.participant.dto.JoinSessionRequest;
import io.github.hjham0856.moasseugi.participant.dto.JoinSessionResponse;
import io.github.hjham0856.moasseugi.participant.dto.ParticipantSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public abstract class ParticipantController {
    @PostMapping("/api/sessions/{sessionId}/participants")
    public abstract JoinSessionResponse joinSession(UUID sessionId, JoinSessionRequest request);

    @GetMapping("/api/sessions/{sessionId}/participants")
    public abstract List<ParticipantSummaryResponse> getParticipants(UUID sessionId);

    // TODO: 회원/비회원 참가 인증, 재접속 토큰 발급 및 닉네임 중복 검증 구현
}
