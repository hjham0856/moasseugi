package io.github.hjham0856.moasseugi.session;

import io.github.hjham0856.moasseugi.session.SessionApiModels.CreateRequest;
import io.github.hjham0856.moasseugi.session.SessionApiModels.CreateResponse;
import io.github.hjham0856.moasseugi.session.SessionApiModels.Detail;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 계정 없는 안건 생성과 참가 키가 필요 없는 공개 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResponse createSession(@Valid @RequestBody CreateRequest request) {
        return sessionService.createSession(request);
    }

    @GetMapping("/{sessionId}")
    public Detail getSession(@PathVariable UUID sessionId) {
        return sessionService.getSession(sessionId);
    }
}
