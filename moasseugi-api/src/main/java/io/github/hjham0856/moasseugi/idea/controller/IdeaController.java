package io.github.hjham0856.moasseugi.idea.controller;

import io.github.hjham0856.moasseugi.idea.dto.IdeaResponse;
import io.github.hjham0856.moasseugi.idea.dto.SaveIdeaRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

@Controller
public abstract class IdeaController {
    @PutMapping("/api/sessions/{sessionId}/ideas")
    public abstract IdeaResponse putIdea(UUID sessionId, SaveIdeaRequest request);

    @GetMapping("/api/sessions/{sessionId}/ideas/me")
    public abstract List<IdeaResponse> getMyIdeas(UUID sessionId);

    @DeleteMapping("/api/sessions/{sessionId}/ideas/{ideaId}")
    public abstract void deleteIdea(UUID sessionId, UUID ideaId);

    // TODO: 참가자 인증, 소유권 확인, 작성 단계 및 최대 3개 제한 구현
}
