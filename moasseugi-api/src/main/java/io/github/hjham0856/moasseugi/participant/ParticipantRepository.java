package io.github.hjham0856.moasseugi.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 안건별 참가 신원과 닉네임 중복 여부를 조회한다.
 */
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, UUID> {

    Optional<ParticipantEntity> findBySession_IdAndParticipantToken(UUID sessionId, String participantToken);

    boolean existsBySession_IdAndNickname(UUID sessionId, String nickname);

    List<ParticipantEntity> findBySession_Id(UUID sessionId);
}
