package io.github.hjham0856.moasseugi.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import io.github.hjham0856.moasseugi.idea.IdeaEntity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 회의의 한 안건을 저장한다. 새 안건은 작성 단계이며 채택 아이디어가 없다.
 */
@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_idea_id",
            foreignKey = @ForeignKey(name = "fk_session_selected_idea"))
    private IdeaEntity selectedIdea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.WRITING;

    protected SessionEntity() {
    }

    /**
     * 작성 단계의 새 안건을 만든다. 생성자의 참가 기록은 별도로 함께 저장해야 한다.
     */
    public SessionEntity(String title, String description) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getSelectedIdeaId() {
        return selectedIdea == null ? null : selectedIdea.getId();
    }

    public IdeaEntity getSelectedIdea() {
        return selectedIdea;
    }

    public SessionStatus getStatus() {
        return status;
    }

    /**
     * 안건을 WRITING → EVALUATING → RESULT 순서로만 이동시킨다.
     * 진행 권한과 전환 조건은 호출자가 먼저 확인해야 한다.
     *
     * @throws IllegalStateException 이미 결과 단계인 경우
     */
    public void advanceStatus() {
        status = switch (status) {
            case WRITING -> SessionStatus.EVALUATING;
            case EVALUATING -> SessionStatus.RESULT;
            case RESULT -> throw new IllegalStateException("결과 단계에서는 더 진행할 수 없습니다.");
        };
    }

    /**
     * 회의에서 논의한 결과로 아이디어 하나를 채택한다. 채택은 한 번뿐이며 단계는 바뀌지 않는다.
     * RESULT 단계 여부와 아이디어가 이 안건에 속하는지는 호출자가 먼저 확인해야 한다.
     *
     * @throws IllegalStateException 이미 채택한 아이디어가 있는 경우
     */
    public void selectIdea(IdeaEntity idea) {
        if (selectedIdea != null) {
            throw new IllegalStateException("이미 채택한 아이디어가 있습니다.");
        }

        selectedIdea = idea;
    }
}
