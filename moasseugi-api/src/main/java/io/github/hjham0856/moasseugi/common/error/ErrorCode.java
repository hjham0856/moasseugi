package io.github.hjham0856.moasseugi.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식 또는 입력값이 올바르지 않습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "마감시각 설정이 올바르지 않습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 정보가 없거나 유효하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "재접속 토큰이 만료되었습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "해당 행동을 수행할 권한이 없습니다."),
    NOT_HOST(HttpStatus.FORBIDDEN, "진행자만 수행할 수 있습니다."),
    NOT_IDEA_OWNER(HttpStatus.FORBIDDEN, "본인의 아이디어만 처리할 수 있습니다."),
    SELF_EVALUATION(HttpStatus.FORBIDDEN, "본인이 작성한 아이디어는 평가할 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다."),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    INVALID_PHASE_ACTION(HttpStatus.CONFLICT, "현재 단계에서는 수행할 수 없습니다."),
    JOIN_CLOSED(HttpStatus.CONFLICT, "참가 가능한 단계가 아닙니다."),
    NICKNAME_TAKEN(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참가한 안건입니다."),
    IDEA_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "아이디어는 3개까지 작성할 수 있습니다."),
    RESULT_NOT_READY(HttpStatus.CONFLICT, "아직 결과를 공개할 단계가 아닙니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
