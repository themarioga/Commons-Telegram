package org.themarioga.commons.telegram.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.themarioga.commons.engine.dao.intf.UserDao;
import org.themarioga.commons.engine.models.Lang;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.services.intf.I18NService;
import org.themarioga.commons.engine.services.intf.UserService;
import org.themarioga.commons.telegram.dao.intf.TelegramUserDao;
import org.themarioga.commons.telegram.models.TelegramUser;
import org.themarioga.commons.telegram.services.impl.TelegramUserServiceImpl;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUserServiceTest {

    private static final long TELEGRAM_ID = 123456789L;
    private static final long OTHER_TELEGRAM_ID = 987654321L;

    @InjectMocks
    private TelegramUserServiceImpl telegramUserService;

    @Mock
    private TelegramUserDao telegramUserDao;
    @Mock
    private UserDao userDao;
    @Mock
    private UserService userService;
    @Mock
    private I18NService i18NService;

    private Lang lang;
    private User user;
    private TelegramUser telegramUser;

    @BeforeEach
    void setUp() {
        lang = new Lang();
        lang.setId("es");
        lang.setName("Español");

        user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        user.setUsername("themarioga");
        user.setName("Mario (@themarioga)");
        user.setActive(true);
        user.setLang(lang);

        telegramUser = new TelegramUser();
        telegramUser.setId(TELEGRAM_ID);
        telegramUser.setUser(user);
        telegramUser.setLanguageCode("es");
    }

    @Test
    void register_createsUserAndMapping() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(null);
        when(i18NService.getLanguage("es")).thenReturn(lang);
        when(userService.createOrReactivate("themarioga", "Mario (@themarioga)", lang)).thenReturn(user);
        when(telegramUserDao.create(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        TelegramUser created = telegramUserService.register(from(TELEGRAM_ID, "themarioga", "Mario", "es"));

        Assertions.assertNotNull(created);
        Assertions.assertEquals(TELEGRAM_ID, created.getId());
        Assertions.assertEquals(user, created.getUser());
        Assertions.assertNotNull(created.getLastSeen());
    }

    /**
     * Un usuario sin alias se identifica por "tg:&lt;id&gt;".
     */
    @Test
    void register_userWithoutAlias_getsSyntheticUsername() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(null);
        when(i18NService.getLanguage(null)).thenReturn(lang);
        when(userService.createOrReactivate("tg:" + TELEGRAM_ID, "Mario", lang)).thenReturn(user);
        when(telegramUserDao.create(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.register(from(TELEGRAM_ID, null, "Mario", null));

        verify(userService).createOrReactivate("tg:" + TELEGRAM_ID, "Mario", lang);
    }

    /**
     * /start repetido no puede fallar: no se crea nada nuevo.
     */
    @Test
    void register_isIdempotent() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        TelegramUser result = telegramUserService.register(from(TELEGRAM_ID, "themarioga", "Mario", "es"));

        Assertions.assertEquals(telegramUser, result);
        verify(userService, never()).createOrReactivate(anyString(), anyString(), any());
    }

    @Test
    void login_unregisteredUser_returnsNull() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(null);

        Assertions.assertNull(telegramUserService.login(from(TELEGRAM_ID, "themarioga", "Mario", "es")));
    }

    @Test
    void login_unchangedUser_touchesNothing() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.login(from(TELEGRAM_ID, "themarioga", "Mario", "es"));

        verify(userService, never()).setUsername(any(), anyString());
        verify(userService, never()).rename(any(), anyString());
    }

    @Test
    void login_renamedUser_refreshesDisplayName() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.login(from(TELEGRAM_ID, "themarioga", "Mario Nuevo", "es"));

        verify(userService).rename(user, "Mario Nuevo (@themarioga)");
        verify(userService, never()).setUsername(any(), anyString());
    }

    /**
     * Telegram trata los alias sin distinguir mayúsculas, así que "TheMarioga" no es un alias nuevo.
     */
    @Test
    void login_aliasCaseChange_isNotAChange() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.login(from(TELEGRAM_ID, "TheMarioga", "Mario", "es"));

        verify(userService, never()).setUsername(any(), anyString());
    }

    @Test
    void login_newAliasNobodyElseHas_isAssignedDirectly() {
        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(userDao.getByUsername("nuevoalias")).thenReturn(null);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.login(from(TELEGRAM_ID, "nuevoalias", "Mario", "es"));

        verify(userService).setUsername(user, "nuevoalias");
        verify(telegramUserDao, never()).flush();
    }

    /**
     * D9: si el alias lo tenía otro, se le degrada a su identificador sintético y el nuevo dueño se
     * lo queda. El flush entre los dos UPDATE es obligatorio por el índice único de Users.username.
     */
    @Test
    void login_aliasHeldByAnotherUser_isStolenInTheRightOrder() {
        User previousOwner = new User();
        previousOwner.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        previousOwner.setUsername("codiciado");
        previousOwner.setName("El de antes");
        previousOwner.setLang(lang);

        TelegramUser previousTelegramUser = new TelegramUser();
        previousTelegramUser.setId(OTHER_TELEGRAM_ID);
        previousTelegramUser.setUser(previousOwner);

        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(userDao.getByUsername("codiciado")).thenReturn(previousOwner);
        when(telegramUserDao.getByUser(previousOwner)).thenReturn(previousTelegramUser);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.login(from(TELEGRAM_ID, "codiciado", "Mario", "es"));

        InOrder inOrder = inOrder(userService, telegramUserDao);
        inOrder.verify(userService).setUsername(previousOwner, "tg:" + OTHER_TELEGRAM_ID);
        inOrder.verify(telegramUserDao).flush();
        inOrder.verify(userService).setUsername(user, "codiciado");
    }

    /**
     * El alias podría estar ocupado por un usuario que no viene de Telegram (otra plataforma): se
     * degrada igual, pero sin poder usar un id de Telegram que no tiene.
     */
    @Test
    void login_aliasHeldByNonTelegramUser_isStolenToo() {
        User previousOwner = new User();
        previousOwner.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        previousOwner.setUsername("codiciado");
        previousOwner.setName("Usuario web");
        previousOwner.setLang(lang);

        when(telegramUserDao.getByIdFetchingUser(TELEGRAM_ID)).thenReturn(telegramUser);
        when(userDao.getByUsername("codiciado")).thenReturn(previousOwner);
        when(telegramUserDao.getByUser(previousOwner)).thenReturn(null);
        when(telegramUserDao.createOrUpdate(any(TelegramUser.class))).thenAnswer(i -> i.getArgument(0));

        telegramUserService.login(from(TELEGRAM_ID, "codiciado", "Mario", "es"));

        verify(userService).setUsername(previousOwner, "tg:" + previousOwner.getId());
        verify(userService).setUsername(user, "codiciado");
    }

    private org.telegram.telegrambots.meta.api.objects.User from(long id, String alias, String firstName, String languageCode) {
        return org.telegram.telegrambots.meta.api.objects.User.builder().id(id).isBot(false).userName(alias).firstName(firstName).languageCode(languageCode).build();
    }

}
