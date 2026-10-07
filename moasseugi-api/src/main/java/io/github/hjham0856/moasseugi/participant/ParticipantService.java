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

/**
 * 작성 중 신규 참가를 등록하고, 진행자에게 참가자 목록을 제공한다.
 */
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

    /**
     * 작성 중인 안건에 일반 참가자를 저장하고 이후 신원 확인에 사용할 키를 반환한다.
     * 닉네임은 앞뒤 공백을 제거한 값으로 해당 안건의 진행자와 참가자 모두에 대해 중복 검사한다.
     *
     * @throws ResponseStatusException 닉네임 오류는 400, 안건이 없으면 404,
     *         작성 종료 또는 닉네임 중복이면 409
     */
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

    /**
     * 진행자에게만 참가자 목록을 반환한다. 다른 사람의 참가 키는 포함하지 않는다.
     *
     * @throws ResponseStatusException 안건이 없으면 404, 키가 유효하지 않으면 401,
     *         일반 참가자이면 403
     */
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

    /**
     * 닉네임 고유 제약 위반만 중복으로 분류하여 다른 무결성 오류를 숨기지 않는다.
     * H2가 제약 이름에 스키마·인덱스 정보를 덧붙이므로 포함 여부로 판별한다.
     */
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

    /**
     * 생성·참가에서 공통으로 사용하는 닉네임 정책을 적용한다.
     * 앞뒤 공백만 제거하며 대소문자와 내부 공백은 유지한다.
     *
     * @throws ResponseStatusException 값이 없거나 공백뿐이거나 정규화 후 30자를 넘으면 400
     */
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
