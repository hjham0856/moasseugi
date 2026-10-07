package io.github.hjham0856.moasseugi.participant;

import io.github.hjham0856.moasseugi.session.SessionEntity;
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
 * 한 안건의 참가 신원이다. 생성자도 참가자로 저장하며 진행자 역할을 함께 부여한다.
 */
@Entity
@Table(name = "participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_participant_token", columnNames = "participant_token"),
        @UniqueConstraint(name = "uk_participant_session_nickname", columnNames = {"session_id", "nickname"})
})
public class ParticipantEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_participant_session"))
    private SessionEntity session;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "participant_token", nullable = false, length = 43)
    private String participantToken;

    @Column(name = "is_host", nullable = false)
    private boolean host;

    protected ParticipantEntity() {
    }

    /**
     * 참가 기록을 만든다. nickname은 공통 정책으로 정규화된 값이어야 한다.
     * host는 안건 생성자에게만 true를 전달한다.
     */
    public ParticipantEntity(SessionEntity session, String nickname, String participantToken, boolean host) {
        this.session = session;
        this.nickname = nickname;
        this.participantToken = participantToken;
        this.host = host;
    }

    public UUID getId() {
        return id;
    }

    public SessionEntity getSession() {
        return session;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isHost() {
        return host;
    }
}
