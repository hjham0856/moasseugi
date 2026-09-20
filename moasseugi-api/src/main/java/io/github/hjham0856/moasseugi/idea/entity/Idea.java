package io.github.hjham0856.moasseugi.idea.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Idea {
    @Id
    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID participantId;
    private String content;

    // TODO: 식별자, 참가자 관계 및 참가자당 최대 3개 제약조건 구현
}
