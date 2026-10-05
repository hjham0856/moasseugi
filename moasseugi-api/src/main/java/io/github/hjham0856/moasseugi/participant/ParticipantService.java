package io.github.hjham0856.moasseugi.participant;

import io.github.hjham0856.moasseugi.participant.ParticipantApiModels.JoinRequest;
import io.github.hjham0856.moasseugi.participant.ParticipantApiModels.JoinResponse;
import io.github.hjham0856.moasseugi.session.SessionApiModels;
import io.github.hjham0856.moasseugi.session.SessionEntity;
import io.github.hjham0856.moasseugi.session.SessionRepository;
import io.github.hjham0856.moasseugi.session.SessionStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ParticipantService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantTokenGenerator participantTokenGenerator;
    private final CurrentParticipantService currentParticipantService;

    public ParticipantService(
            SessionRepository sessionRepository,
            ParticipantRepository participantRepository,
            ParticipantTokenGenerator participantTokenGenerator,
            CurrentParticipantService currentParticipantService
    ) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.participantTokenGenerator = participantTokenGenerator;
        this.currentParticipantService = currentParticipantService;
    }

    @Transactional
    public JoinResponse joinSession(UUID sessionId, JoinRequest request) {
        String nickname = normalizeNickname(request.nickname());
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다."));
        if (session.getStatus() != SessionStatus.WRITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "작성 단계에서만 참가할 수 있습니다.");
        }
        if (participantRepository.existsBySession_IdAndNickname(sessionId, nickname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        String participantToken = participantTokenGenerator.generate();
        try {
            ParticipantEntity participant = participantRepository.saveAndFlush(
                    new ParticipantEntity(session, nickname, participantToken, false));
            return new JoinResponse(toSummary(participant), participantToken);
        } catch (DataIntegrityViolationException e) {
            // 실패한 트랜잭션에서 재조회하지 않고 위반된 제약 이름으로만 동시 중복을 판별한다.
            if (isNicknameUniqueViolation(e)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
            }
            throw e;
        }
    }

    public List<SessionApiModels.ParticipantSummary> getParticipants(UUID sessionId, String participantToken) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "안건을 찾을 수 없습니다.");
        }
        currentParticipantService.requireHost(sessionId, participantToken);
        return participantRepository.findBySession_Id(sessionId).stream()
                .map(this::toSummary)
                .toList();
    }

    private SessionApiModels.ParticipantSummary toSummary(ParticipantEntity participant) {
        return new SessionApiModels.ParticipantSummary(
                participant.getId(), participant.getNickname(), participant.isHost());
    }

    // (session_id, nickname) UNIQUE 위반만 중복으로 취급하고 토큰 null 등 다른 무결성 위반은 그대로 전달한다.
    // H2는 제약 이름에 스키마·인덱스 정보를 덧붙여 반환하므로 포함 여부로 판별한다.
    private boolean isNicknameUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                return name != null
                        && name.toLowerCase(Locale.ROOT).contains("uk_participant_session_nickname");
            }
            cause = cause.getCause();
        }
        return false;
    }

    // ' 민수 '와 '민수'를 같은 닉네임으로 취급하고 'Alex'와 'alex'는 구분한다. 내부 공백과 문자 종류는 제한하지 않는다.
    public static String normalizeNickname(String rawNickname) {
        if (rawNickname == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임이 필요합니다.");
        }
        String nickname = rawNickname.strip();
        if (nickname.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임이 필요합니다.");
        }
        if (nickname.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 30자까지 입력할 수 있습니다.");
        }
        return nickname;
    }
}
