package io.github.hjham0856.moasseugi.session.controller;

import io.github.hjham0856.moasseugi.session.dto.CreateSessionRequest;
import io.github.hjham0856.moasseugi.session.dto.SessionDetailResponse;
import io.github.hjham0856.moasseugi.session.dto.SessionSummaryResponse;
import io.github.hjham0856.moasseugi.session.dto.UpdateSessionRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public abstract class SessionController {
    @PostMapping("/api/sessions")
    public abstract SessionDetailResponse createSession(CreateSessionRequest request);

    @GetMapping("/api/sessions/hosted")
    public abstract List<SessionSummaryResponse> getHostedSessions();

    @GetMapping("/api/sessions/participated")
    public abstract List<SessionSummaryResponse> getParticipatedSessions();

    @GetMapping("/api/sessions/{sessionId}")
    public abstract SessionDetailResponse getSession(UUID sessionId);

    @PatchMapping("/api/sessions/{sessionId}")
    public abstract SessionDetailResponse updateSession(UUID sessionId, UpdateSessionRequest request);

    @PostMapping("/api/sessions/{sessionId}/close-writing")
    public abstract SessionDetailResponse closeWriting(UUID sessionId);

    @PostMapping("/api/sessions/{sessionId}/close-evaluation")
    public abstract SessionDetailResponse closeEvaluation(UUID sessionId);

    // TODO: 진행자 권한, 상태 전이 및 마감 시각 검증 구현
}
