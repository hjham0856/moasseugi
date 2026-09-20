package io.github.hjham0856.moasseugi.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class User {
    @Id
    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private String username;
    private String passwordHash;

    // TODO: 식별자, 컬럼 제약조건 및 username 고유성 매핑
}
