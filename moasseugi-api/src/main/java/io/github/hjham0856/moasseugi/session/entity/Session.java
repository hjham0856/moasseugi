package io.github.hjham0856.moasseugi.session.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Session {
    @Id
    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID hostUserId;
    private String title;
    private String description;
    private SessionStatus status;
    private Instant writingEndsAt;
    private Instant evaluationEndsAt;
    private UUID selectedIdeaId;

    // TODO: 식별자, 관계, 상태 전이 및 마감 시각/선택 아이디어 제약조건 매핑
}
