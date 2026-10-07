package io.github.hjham0856.moasseugi.idea;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdeaRepository extends JpaRepository<IdeaEntity, UUID> {

    Optional<IdeaEntity> findByParticipant_Id(UUID participantId);
}
