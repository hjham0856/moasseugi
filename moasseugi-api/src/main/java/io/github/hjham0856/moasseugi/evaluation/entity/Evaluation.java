package io.github.hjham0856.moasseugi.evaluation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Evaluation {
    @Id
    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID participantId;
    private UUID ideaId;
    private Short value;

    // TODO: 식별자, 관계, 평가값 범위 및 참가자-아이디어 고유성 제약조건 매핑
}
