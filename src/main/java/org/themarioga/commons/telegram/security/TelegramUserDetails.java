package org.themarioga.commons.telegram.security;

import org.themarioga.commons.engine.security.UserDetails;
import org.themarioga.commons.engine.security.UserRole;
import org.themarioga.commons.telegram.models.TelegramUser;

/**
 * Principal de seguridad de una petición que llega desde Telegram: el usuario del motor (que es lo
 * que leen {@code CAHServiceImpl} e {@code I18NServiceImpl}) más los datos propios de Telegram.
 */
public class TelegramUserDetails extends UserDetails {

    private final Long telegramId;
    private final String languageCode;

    public TelegramUserDetails(TelegramUser telegramUser, UserRole role) {
        super(telegramUser.getUser(), role);

        this.telegramId = telegramUser.getId();
        this.languageCode = telegramUser.getLanguageCode();
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

}
