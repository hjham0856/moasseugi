package io.github.hjham0856.moasseugi.result;

import io.github.hjham0856.moasseugi.result.ResultApiModels.Response;
import io.github.hjham0856.moasseugi.result.ResultApiModels.SelectRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 평가 분포 결과의 공개 조회와 진행자의 1회 채택 API를 제공한다.
 */
@RestController
@RequestMapping("/sessions/{sessionId}")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/results")
    public Response getResults(@PathVariable UUID sessionId) {
        return resultService.getResults(sessionId);
    }

    @PostMapping("/select-idea")
    public Response selectIdea(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken,
            @Valid @RequestBody SelectRequest request
    ) {
        return resultService.selectIdea(sessionId, participantToken, request);
    }
}
