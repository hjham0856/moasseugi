package io.github.hjham0856.moasseugi.result.controller;

import io.github.hjham0856.moasseugi.result.dto.ResultResponse;
import io.github.hjham0856.moasseugi.result.dto.SelectIdeaRequest;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public abstract class ResultController {
    @GetMapping("/api/sessions/{sessionId}/results")
    public abstract ResultResponse getResults(UUID sessionId);

    @PostMapping("/api/sessions/{sessionId}/select-idea")
    public abstract ResultResponse selectIdea(UUID sessionId, SelectIdeaRequest request);

    // TODO: 결과 공개 단계, 진행자 권한 및 최종 채택 불변성 구현
}
