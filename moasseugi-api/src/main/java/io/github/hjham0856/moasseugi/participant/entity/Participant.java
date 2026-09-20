package io.github.hjham0856.moasseugi.participant.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Participant {
    @Id
    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID sessionId;
    private UUID userId;
    private String nickname;
    private String reconnectToken;

    // TODO: 식별자, 관계 및 세션별 닉네임/사용자/재접속 토큰 고유성 매핑
}
