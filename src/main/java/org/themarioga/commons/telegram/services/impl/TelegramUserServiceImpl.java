package org.themarioga.commons.telegram.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.commons.engine.dao.intf.UserDao;
import org.themarioga.commons.engine.exceptions.ApplicationException;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.services.intf.I18NService;
import org.themarioga.commons.engine.services.intf.UserService;
import org.themarioga.commons.telegram.dao.intf.TelegramUserDao;
import org.themarioga.commons.telegram.models.TelegramUser;
import org.themarioga.commons.telegram.services.intf.TelegramUserService;
import org.themarioga.commons.telegram.util.BotMessageUtils;
import org.themarioga.commons.telegram.util.TelegramUserUtils;

import java.util.Date;

@Service
public class TelegramUserServiceImpl implements TelegramUserService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramUserServiceImpl.class);

    private final TelegramUserDao telegramUserDao;
    private final UserDao userDao;
    private final UserService userService;
    private final I18NService i18NService;

    @Autowired
    public TelegramUserServiceImpl(TelegramUserDao telegramUserDao, UserDao userDao, UserService userService, I18NService i18NService) {
        this.telegramUserDao = telegramUserDao;
        this.userDao = userDao;
        this.userService = userService;
        this.i18NService = i18NService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public TelegramUser register(org.telegram.telegrambots.meta.api.objects.User from) {
        logger.debug("Registering telegram user {}", from.getId());

        TelegramUser existing = telegramUserDao.getByIdFetchingUser(from.getId());
        if (existing != null) {
            // /start repetido: no es un error, solo refrescamos lo que haya cambiado
            return refresh(existing, from);
        }

        User user = userService.createOrReactivate(TelegramUserUtils.usernameOf(from), BotMessageUtils.getUsername(from), i18NService.getLanguage(from.getLanguageCode()));

        TelegramUser telegramUser = new TelegramUser();
        telegramUser.setId(from.getId());
        telegramUser.setUser(user);
        telegramUser.setLanguageCode(from.getLanguageCode());
        telegramUser.setLastSeen(new Date());

        return telegramUserDao.create(telegramUser);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public TelegramUser login(org.telegram.telegrambots.meta.api.objects.User from) {
        TelegramUser telegramUser = telegramUserDao.getByIdFetchingUser(from.getId());
        if (telegramUser == null) {
            logger.debug("Telegram user {} is not registered yet", from.getId());
            return null;
        }

        return refresh(telegramUser, from);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public TelegramUser getByTelegramId(Long telegramId) {
        return telegramUserDao.getByIdFetchingUser(telegramId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public TelegramUser getByUser(User user) {
        return telegramUserDao.getByUser(user);
    }

    /**
     * Pone al día la identidad y el nombre visible del usuario a partir de lo que reporta Telegram.
     */
    private TelegramUser refresh(TelegramUser telegramUser, org.telegram.telegrambots.meta.api.objects.User from) {
        User user = telegramUser.getUser();

        String username = TelegramUserUtils.usernameOf(from);
        if (!username.equals(user.getUsername())) {
            releaseUsername(username, user);

            userService.setUsername(user, username);
        }

        String displayName = BotMessageUtils.getUsername(from);
        if (!displayName.equals(user.getName())) {
            userService.rename(user, displayName);
        }

        telegramUser.setLanguageCode(from.getLanguageCode());
        telegramUser.setLastSeen(new Date());

        return telegramUserDao.createOrUpdate(telegramUser);
    }

    /**
     * Un usuario puede liberar su alias y otro cogerlo. Cuando eso pasa, el alias todavía figura en la
     * ficha del dueño anterior: se le degrada a su identificador sintético para que el dueño actual
     * pueda quedárselo (Telegram garantiza un único dueño vivo, así que el estado refleja la realidad).
     */
    private void releaseUsername(String username, User newOwner) {
        User previousOwner = userDao.getByUsername(username);
        if (previousOwner == null || previousOwner.getId().equals(newOwner.getId())) return;

        TelegramUser previousTelegramUser = telegramUserDao.getByUser(previousOwner);
        String fallback = previousTelegramUser != null ? TelegramUserUtils.syntheticUsernameOf(previousTelegramUser.getId()) : TelegramUserUtils.SYNTHETIC_PREFIX + previousOwner.getId();

        logger.info("Username {} changed hands: demoting previous owner {} to {}", username, previousOwner.getId(), fallback);

        userService.setUsername(previousOwner, fallback);

        // Sin este flush, Hibernate puede ordenar los dos UPDATE al revés y chocar con el índice
        // único de Users.username.
        telegramUserDao.flush();
    }

}
