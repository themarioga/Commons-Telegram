package org.themarioga.commons.telegram.util;

import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Locale;

/**
 * Traducción entre la identidad de Telegram y la identidad del motor.
 * <p>
 * Ojo con la nomenclatura: {@link BotMessageUtils#getUsername(User)} devuelve el <em>nombre
 * visible</em> ("Nombre Apellido (@alias)"), que va a {@code User.name}. El identificador estable y
 * único que va a {@code User.username} es el que calcula esta clase.
 */
public class TelegramUserUtils {

    /**
     * Prefijo de los identificadores sintéticos. Telegram no admite ':' en los alias, así que no
     * puede colisionar con un alias real.
     */
    public static final String SYNTHETIC_PREFIX = "tg:";

    private TelegramUserUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Identidad del usuario en el motor: su alias en minúsculas (Telegram los trata sin distinguir
     * mayúsculas, así que normalizarlos es lo que hace que el índice único se comporte como espera
     * el usuario) o "tg:&lt;id&gt;" si no tiene alias.
     */
    public static String usernameOf(User from) {
        return StringUtils.hasText(from.getUserName()) ? from.getUserName().toLowerCase(Locale.ROOT) : syntheticUsernameOf(from.getId());
    }

    public static String syntheticUsernameOf(Long telegramId) {
        return SYNTHETIC_PREFIX + telegramId;
    }

    /**
     * Normaliza un alias tecleado por una persona (en /deletegamebyusername o /add_collab), que puede
     * venir con '@' delante y en cualquier combinación de mayúsculas.
     */
    public static String normalizeUsername(String input) {
        if (input == null) return null;

        String normalized = input.trim();
        if (normalized.startsWith("@")) normalized = normalized.substring(1);

        return normalized.toLowerCase(Locale.ROOT);
    }

}
