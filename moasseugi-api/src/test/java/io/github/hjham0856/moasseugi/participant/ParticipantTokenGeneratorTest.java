package io.github.hjham0856.moasseugi.participant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 참가 키의 저장 길이·URL 안전 형식과 호출별 새 값 발급을 확인한다.
 */
class ParticipantTokenGeneratorTest {

    @Test
    void generatesUrlSafeTokenWithExpectedLength() {
        // given: 참가 키는 DB의 43자 제한을 만족해야 하고 URL에서도 안전해야 한다.
        ParticipantTokenGenerator generator = new ParticipantTokenGenerator();

        // when: 서로 다른 참가에 사용할 키를 각각 발급한다.
        String token = generator.generate();
        String anotherToken = generator.generate();

        // then: 두 키는 저장 길이에 맞고 URL에 안전하며 서로 다르다.
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(token, anotherToken);
    }
}
