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

    public void updateContent(String content) {
        this.content = content;
    }
}
