package org.themarioga.telegram.commons.security;

import org.themarioga.engine.commons.security.UserDetails;
import org.themarioga.engine.commons.security.UserRole;
import org.themarioga.telegram.commons.models.TelegramUser;

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
