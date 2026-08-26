package org.themarioga.commons.telegram.services.intf;

import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.telegram.models.TelegramUser;

public interface TelegramUserService {

    /**
     * Da de alta al usuario en el motor y crea su equivalencia. Idempotente: si ya estaba registrado
     * refresca sus datos en lugar de fallar.
     */
    TelegramUser register(org.telegram.telegrambots.meta.api.objects.User from);

    /**
     * Carga la sesión del usuario y refresca su nombre visible y su alias.
     *
     * @return {@code null} si el usuario nunca ha hecho /start.
     */
    TelegramUser login(org.telegram.telegrambots.meta.api.objects.User from);

    TelegramUser getByTelegramId(Long telegramId);

    TelegramUser getByUser(User user);

}
