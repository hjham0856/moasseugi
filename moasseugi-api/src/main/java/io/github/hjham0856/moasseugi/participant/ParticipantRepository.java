package io.github.hjham0856.moasseugi.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<ParticipantEntity, UUID> {

    Optional<ParticipantEntity> findBySession_IdAndReconnectToken(UUID sessionId, String reconnectToken);
}
