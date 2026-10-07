package io.github.hjham0856.moasseugi.idea;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 아이디어 소유자인 참가자를 기준으로 저장 기록을 조회한다.
 */
public interface IdeaRepository extends JpaRepository<IdeaEntity, UUID> {

    Optional<IdeaEntity> findByParticipant_Id(UUID participantId);

    List<IdeaEntity> findByParticipant_Session_IdAndParticipant_IdNot(UUID sessionId, UUID participantId);

    Optional<IdeaEntity> findByIdAndParticipant_Session_Id(UUID id, UUID sessionId);

    boolean existsByIdAndParticipant_Id(UUID id, UUID participantId);
}
