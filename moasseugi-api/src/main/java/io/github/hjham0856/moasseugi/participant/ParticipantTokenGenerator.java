package io.github.hjham0856.moasseugi.participant;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 비로그인 참가자를 식별할 추측하기 어려운 랜덤 키를 발급한다.
 */
@Component
public class ParticipantTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 32바이트 난수를 패딩 없는 URL 안전 Base64로 인코딩한 43자 키를 반환한다.
     */
    public String generate() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
