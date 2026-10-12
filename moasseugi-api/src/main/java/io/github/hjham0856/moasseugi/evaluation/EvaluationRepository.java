package io.github.hjham0856.moasseugi.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 참가자의 현재 평가를 아이디어 조합으로 조회한다.
 */
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, UUID> {

    Optional<EvaluationEntity> findByParticipant_IdAndIdea_Id(UUID participantId, UUID ideaId);

    List<EvaluationEntity> findByParticipant_IdAndIdea_IdIn(UUID participantId, Collection<UUID> ideaIds);
}
