package io.github.hjham0856.moasseugi.idea;

import io.github.hjham0856.moasseugi.idea.IdeaApiModels.Response;
import io.github.hjham0856.moasseugi.idea.IdeaApiModels.WriteRequest;
import io.github.hjham0856.moasseugi.participant.CurrentParticipantService;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import io.github.hjham0856.moasseugi.session.SessionStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IdeaService {

    private final SessionRepository sessionRepository;
    private final IdeaRepository ideaRepository;
    private final CurrentParticipantService currentParticipantService;

    public IdeaService(
            SessionRepository sessionRepository,
            IdeaRepository ideaRepository,
            CurrentParticipantService currentParticipantService
    ) {
        this.sessionRepository = sessionRepository;
        this.ideaRepository = ideaRepository;
        this.currentParticipantService = currentParticipantService;
    }

    @Transactional
    public Response putIdea(UUID sessionId, String participantToken, WriteRequest request) {
        ParticipantEntity participant = requireParticipant(sessionId, participantToken);
        requireWriting(participant);

        Optional<IdeaEntity> existing = ideaRepository.findByParticipant_Id(participant.getId());
        if (existing.isPresent()) {
            IdeaEntity idea = existing.get();
            idea.updateContent(request.content());
            return toResponse(idea);
        }

        try {
            return toResponse(ideaRepository.saveAndFlush(new IdeaEntity(participant, request.content())));
        } catch (DataIntegrityViolationException e) {
            if (isParticipantUniqueViolation(e)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "아이디어가 동시에 저장되었습니다. 다시 저장해 주세요.");
            }
            throw e;
        }
    }

    public Optional<Response> getMyIdea(UUID sessionId, String participantToken) {
        ParticipantEntity participant = requireParticipant(sessionId, participantToken);
        return ideaRepository.findByParticipant_Id(participant.getId()).map(this::toResponse);
    }

    private ParticipantEntity requireParticipant(UUID sessionId, String participantToken) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다.");
        }
        return currentParticipantService.requireParticipant(sessionId, participantToken);
    }

    private void requireWriting(ParticipantEntity participant) {
        if (participant.getSession().getStatus() != SessionStatus.WRITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "작성 단계에서만 아이디어를 저장할 수 있습니다.");
        }
    }

    private Response toResponse(IdeaEntity idea) {
        return new Response(idea.getId(), idea.getContent());
    }

    private boolean isParticipantUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                return name != null && name.toLowerCase(Locale.ROOT).contains("uk_idea_participant");
            }
            cause = cause.getCause();
        }
        return false;
    }
}
