package io.github.hjham0856.moasseugi.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 참가 정보와 별개로 안건을 저장·조회한다.
 */
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
}
