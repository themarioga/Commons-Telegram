package org.themarioga.telegram.commons.models;

import jakarta.persistence.*;
import org.themarioga.engine.commons.models.User;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Equivalencia entre un usuario de Telegram y el usuario del motor.
 * <p>
 * Es compartida por todos los bots de Telegram (CAH y SH): quien se registra en uno ya está
 * registrado en el otro. El alias y el nombre visible no se guardan aquí, viven en
 * {@link User#getUsername()} y {@link User#getName()}.
 */
@Entity
@Table(name = "telegram_user")
public class TelegramUser implements Serializable {

    /**
     * Id de usuario de Telegram. Es la clave: la asigna Telegram, no se genera.
     */
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "language_code", length = 8)
    private String languageCode;

    @Column(name = "last_seen")
    private Date lastSeen;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TelegramUser that = (TelegramUser) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "TelegramUser{id=" + id + ", user=" + user + '}';
    }

}
