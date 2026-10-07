package io.github.hjham0856.moasseugi.idea;

import io.github.hjham0856.moasseugi.idea.IdeaApiModels.Response;
import io.github.hjham0856.moasseugi.idea.IdeaApiModels.WriteRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.Optional;

/**
 * 참가 키로 본인의 아이디어만 저장·조회하는 API를 제공한다.
 */
@RestController
@RequestMapping("/sessions/{sessionId}/ideas")
public class IdeaController {

    private final IdeaService ideaService;

    public IdeaController(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    @PutMapping
    public Response putIdea(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken,
            @Valid @RequestBody WriteRequest request
    ) {
        return ideaService.putIdea(sessionId, participantToken, request);
    }

    /**
     * 저장 전에는 빈 HTTP 본문이 아닌 JSON null을 반환한다.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyIdea(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken
    ) {
        Optional<Response> idea = ideaService.getMyIdea(sessionId, participantToken);

        if (idea.isEmpty()) {
            // 자바 null 반환은 빈 응답이 되므로 계약의 JSON null을 그대로 보낸다.
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("null");
        }

        return ResponseEntity.ok(idea.get());
    }
}
