package org.themarioga.commons.telegram.security;

import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.security.SecurityUtils;

/**
 * Punto de acceso único a los datos de la petición de Telegram: la identidad (que vive en el
 * contexto de Spring Security, porque es lo que lee el motor) y el chat (que vive en el
 * {@link TelegramContextHolder}). Con esto, el código de los bots no toca ninguno de los dos
 * directamente.
 */
public class TelegramSecurityUtils {

    private TelegramSecurityUtils() {
        throw new UnsupportedOperationException();
    }

    // ///////////// Identidad //////////////////

    /**
     * @return {@code true} si el usuario ha hecho /start alguna vez y por tanto tiene sesión.
     */
    public static boolean isRegistered() {
        return getTelegramUserDetails() != null;
    }

    public static User getUser() {
        return SecurityUtils.getUser();
    }

    /**
     * Id de usuario de Telegram: el de la sesión si está registrado, o el del update en curso si no
     * lo está (que es lo que necesita /start para darle de alta).
     */
    public static Long getTelegramId() {
        TelegramUserDetails userDetails = getTelegramUserDetails();
        if (userDetails != null) return userDetails.getTelegramId();

        TelegramContext context = TelegramContextHolder.get();
        return context != null && context.getFrom() != null ? context.getFrom().getId() : null;
    }

    /**
     * Identidad en el motor: el alias de Telegram en minúsculas, o "tg:&lt;id&gt;" si no tiene alias.
     */
    public static String getUsername() {
        return SecurityUtils.getUsername();
    }

    /**
     * Nombre visible, para pintar en los mensajes.
     */
    public static String getDisplayName() {
        return SecurityUtils.getName();
    }

    public static String getLanguageCode() {
        TelegramUserDetails userDetails = getTelegramUserDetails();
        if (userDetails != null) return userDetails.getLanguageCode();

        TelegramContext context = TelegramContextHolder.get();
        return context != null && context.getFrom() != null ? context.getFrom().getLanguageCode() : null;
    }

    public static boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }

    public static TelegramUserDetails getTelegramUserDetails() {
        return SecurityUtils.getUserDetails() instanceof TelegramUserDetails telegramUserDetails
                ? telegramUserDetails
                : null;
    }

    // ///////////// Chat //////////////////

    public static Long getChatId() {
        TelegramContext context = TelegramContextHolder.get();
        return context != null ? context.getChatId() : null;
    }

    public static String getChatTitle() {
        TelegramContext context = TelegramContextHolder.get();
        return context != null ? context.getChatTitle() : null;
    }

    public static Integer getMessageId() {
        TelegramContext context = TelegramContextHolder.get();
        return context != null ? context.getMessageId() : null;
    }

    public static String getCallbackQueryId() {
        TelegramContext context = TelegramContextHolder.get();
        return context != null ? context.getCallbackQueryId() : null;
    }

    public static boolean isPrivate() {
        TelegramContext context = TelegramContextHolder.get();
        return context != null && context.isPrivate();
    }

    /**
     * Sala del motor desde la que se está jugando, o {@code null} si el chat es privado. Se resuelve
     * la primera vez que se pide y se cachea durante la petición.
     */
    public static Room getRoom() {
        TelegramContext context = TelegramContextHolder.get();
        return context != null ? context.getRoom() : null;
    }

}
