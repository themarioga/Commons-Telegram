package org.themarioga.commons.telegram.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.themarioga.commons.engine.security.SecurityUtils;
import org.themarioga.commons.engine.security.UserRole;
import org.themarioga.commons.telegram.config.TelegramAdmins;
import org.themarioga.commons.telegram.models.TelegramUser;
import org.themarioga.commons.telegram.models.UpdateInterceptor;
import org.themarioga.commons.telegram.security.TelegramContext;
import org.themarioga.commons.telegram.security.TelegramContextHolder;
import org.themarioga.commons.telegram.security.TelegramUserDetails;
import org.themarioga.commons.telegram.services.intf.TelegramRoomResolver;
import org.themarioga.commons.telegram.services.intf.TelegramUserService;



/**
 * Monta la sesión de cada update y —lo importante— la desmonta después.
 * <p>
 * El usuario se resuelve una sola vez por update y se deja en el contexto de Spring Security, que es
 * de donde lo lee el motor ({@code CAHServiceImpl}, {@code I18NServiceImpl}). Un usuario que nunca ha
 * hecho /start no tiene sesión: el handler correspondiente decide qué hacer con él.
 */
@Component
public class AuthUpdateInterceptor implements UpdateInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthUpdateInterceptor.class);

    private final TelegramUserService telegramUserService;
    private final ObjectProvider<TelegramRoomResolver> roomResolver;
    private final TelegramAdmins admins;

    @Autowired
    public AuthUpdateInterceptor(TelegramUserService telegramUserService, ObjectProvider<TelegramRoomResolver> roomResolver,
                                 TelegramAdmins admins) {
        this.telegramUserService = telegramUserService;
        this.roomResolver = roomResolver;
        this.admins = admins;
    }

    @Override
    public void before(Update update, String botName) {
        TelegramContext context = TelegramContext.from(update, botName, roomResolver.getIfAvailable());
        TelegramContextHolder.set(context);

        if (context.getFrom() == null) return;

        try {
            TelegramUser telegramUser = telegramUserService.login(context.getFrom());
            if (telegramUser != null) {
                SecurityUtils.setUserDetails(new TelegramUserDetails(telegramUser, roleOf(telegramUser.getId())));
            }
        } catch (Exception e) {
            // Que no se pueda montar la sesión no debe tumbar el update: el handler responderá con el
            // error que corresponda al ver que no hay usuario.
            logger.error("Error al iniciar la sesión del usuario de telegram {}: {}", context.getFrom().getId(), e.getMessage(), e);
        }
    }

    @Override
    public void after(Update update) {
        SecurityContextHolder.clearContext();
        TelegramContextHolder.clear();
    }

    private UserRole roleOf(Long telegramId) {
        return admins.contains(telegramId) ? UserRole.ADMIN : UserRole.USER;
    }

}
