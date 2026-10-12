package io.github.hjham0856.moasseugi.evaluation;

import io.github.hjham0856.moasseugi.idea.IdeaEntity;
import io.github.hjham0856.moasseugi.participant.ParticipantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * 한 참가자가 한 아이디어에 남긴 현재 평가를 저장한다.
 */
@Entity
@Table(name = "evaluations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evaluation_participant_idea", columnNames = {"participant_id", "idea_id"})
})
public class EvaluationEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evaluation_participant"))
    private ParticipantEntity participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evaluation_idea"))
    private IdeaEntity idea;

    @Column(name = "\"value\"", nullable = false)
    private short value;

    protected EvaluationEntity() {
    }

    /**
     * 아이디어 평가를 만든다. 안건 단계·소유자·평가값 검증은 저장 전에 수행해야 한다.
     */
    public EvaluationEntity(ParticipantEntity participant, IdeaEntity idea, short value) {
        this.participant = participant;
        this.idea = idea;
        this.value = value;
    }

    public UUID getIdeaId() {
        return idea.getId();
    }

    public short getValue() {
        return value;
    }

    /**
     * 새 행을 만들지 않고 기존 참가자·아이디어 조합의 평가만 바꾼다.
     */
    public void updateValue(short value) {
        this.value = value;
    }
}
