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

@Entity
@Table(name = "participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_participant_reconnect_token", columnNames = "reconnect_token"),
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

    @Column(name = "reconnect_token", nullable = false, length = 43)
    private String reconnectToken;

    @Column(name = "is_host", nullable = false)
    private boolean host;

    protected ParticipantEntity() {
    }

    public ParticipantEntity(SessionEntity session, String nickname, String reconnectToken, boolean host) {
        this.session = session;
        this.nickname = nickname;
        this.reconnectToken = reconnectToken;
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
