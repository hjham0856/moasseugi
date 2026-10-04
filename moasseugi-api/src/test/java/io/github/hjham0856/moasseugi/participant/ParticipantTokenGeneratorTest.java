package io.github.hjham0856.moasseugi.participant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticipantTokenGeneratorTest {

    @Test
    void generatesUrlSafeTokenWithExpectedLength() {
        ParticipantTokenGenerator generator = new ParticipantTokenGenerator();
        String token = generator.generate();
        String anotherToken = generator.generate();

        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(token, anotherToken);
    }
}
