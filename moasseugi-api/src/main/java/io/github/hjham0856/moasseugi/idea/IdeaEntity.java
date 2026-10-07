package io.github.hjham0856.moasseugi.idea;

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
 * 한 참가자의 아이디어를 저장한다. 참가자별 하나라는 규칙은 DB의 고유 제약으로도 보장한다.
 */
@Entity
@Table(name = "ideas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idea_participant", columnNames = "participant_id")
})
public class IdeaEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_idea_participant"))
    private ParticipantEntity participant;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    protected IdeaEntity() {
    }

    /**
     * 참가자의 첫 아이디어를 만든다. 참가 신원·작성 단계·본문 검증은 저장 전에 수행해야 한다.
     */
    public IdeaEntity(ParticipantEntity participant, String content) {
        this.participant = participant;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    /**
     * 아이디어 ID와 소유자는 유지하고 내용만 바꾼다. 작성 단계 확인은 호출자가 수행한다.
     */
    public void updateContent(String content) {
        this.content = content;
    }
}
