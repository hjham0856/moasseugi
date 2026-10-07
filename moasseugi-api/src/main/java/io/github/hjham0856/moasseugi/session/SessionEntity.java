package io.github.hjham0856.moasseugi.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

    @Column(name = "selected_idea_id")
    private UUID selectedIdeaId;

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
        return selectedIdeaId;
    }

    public SessionStatus getStatus() {
        return status;
    }
}
