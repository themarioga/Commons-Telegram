package org.themarioga.telegram.commons.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.themarioga.engine.commons.models.Lang;
import org.themarioga.engine.commons.models.Room;
import org.themarioga.engine.commons.models.User;
import org.themarioga.engine.commons.security.SecurityUtils;
import org.themarioga.telegram.commons.models.TelegramUser;
import org.themarioga.telegram.commons.security.TelegramContextHolder;
import org.themarioga.telegram.commons.security.TelegramSecurityUtils;
import org.themarioga.telegram.commons.services.impl.AuthUpdateInterceptor;
import org.themarioga.telegram.commons.services.intf.TelegramRoomResolver;
import org.themarioga.telegram.commons.services.intf.TelegramUserService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUpdateInterceptorTest {

    private static final long TELEGRAM_ID = 123456789L;
    private static final long GROUP_CHAT_ID = -100123L;

    @Mock
    private TelegramUserService telegramUserService;
    @Mock
    private ObjectProvider<TelegramRoomResolver> roomResolverProvider;
    @Mock
    private TelegramRoomResolver roomResolver;

    private AuthUpdateInterceptor interceptor;
    private TelegramUser telegramUser;

    @BeforeEach
    void setUp() {
        Lang lang = new Lang();
        lang.setId("es");
        lang.setName("Español");

        User user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        user.setUsername("themarioga");
        user.setName("Mario (@themarioga)");
        user.setActive(true);
        user.setLang(lang);

        telegramUser = new TelegramUser();
        telegramUser.setId(TELEGRAM_ID);
        telegramUser.setUser(user);
        telegramUser.setLanguageCode("es");

        interceptor = new AuthUpdateInterceptor(telegramUserService, roomResolverProvider, List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TelegramContextHolder.clear();
    }

    @Test
    void beforeRegisteredUser_setsSession() {
        when(roomResolverProvider.getIfAvailable()).thenReturn(null);
        when(telegramUserService.login(any())).thenReturn(telegramUser);

        interceptor.before(privateMessage(), "cclhbot");

        Assertions.assertTrue(TelegramSecurityUtils.isRegistered());
        Assertions.assertEquals(TELEGRAM_ID, TelegramSecurityUtils.getTelegramId());
        Assertions.assertEquals("themarioga", TelegramSecurityUtils.getUsername());
        Assertions.assertEquals("Mario (@themarioga)", TelegramSecurityUtils.getDisplayName());
        Assertions.assertEquals("es", SecurityUtils.getLang().getId());
        Assertions.assertTrue(TelegramSecurityUtils.isPrivate());
        Assertions.assertFalse(TelegramSecurityUtils.isAdmin());
    }

    /**
     * Sin /start previo no hay sesión, pero el contexto del chat sí está montado: es lo que necesita
     * el handler de /start para dar de alta al usuario, y lo que permite responder "haz /start" al
     * resto de comandos.
     */
    @Test
    void beforeUnregisteredUser_leavesNoSessionButKeepsContext() {
        when(roomResolverProvider.getIfAvailable()).thenReturn(null);
        when(telegramUserService.login(any())).thenReturn(null);

        interceptor.before(privateMessage(), "cclhbot");

        Assertions.assertFalse(TelegramSecurityUtils.isRegistered());
        Assertions.assertNull(SecurityUtils.getUser());
        Assertions.assertEquals(TELEGRAM_ID, TelegramSecurityUtils.getTelegramId(), "el id debe salir del update");
        Assertions.assertEquals(TELEGRAM_ID, TelegramSecurityUtils.getChatId());
    }

    /**
     * El riesgo R2: en long-polling un único hilo atiende todos los updates, así que un contexto sin
     * limpiar se lo encuentra el siguiente usuario.
     */
    @Test
    void after_clearsEverything() {
        when(roomResolverProvider.getIfAvailable()).thenReturn(null);
        when(telegramUserService.login(any())).thenReturn(telegramUser);

        Update update = privateMessage();
        interceptor.before(update, "cclhbot");
        interceptor.after(update);

        Assertions.assertFalse(TelegramSecurityUtils.isRegistered());
        Assertions.assertNull(SecurityUtils.getUserDetails());
        Assertions.assertNull(TelegramContextHolder.get());
        Assertions.assertNull(TelegramSecurityUtils.getChatId());
        Assertions.assertNull(TelegramSecurityUtils.getRoom());
    }

    /**
     * Si el login revienta, el update debe seguir su curso sin sesión en lugar de tumbar el bot.
     */
    @Test
    void beforeFailingLogin_doesNotPropagate() {
        when(roomResolverProvider.getIfAvailable()).thenReturn(null);
        when(telegramUserService.login(any())).thenThrow(new IllegalStateException("BD caída"));

        Update update = privateMessage();
        Assertions.assertDoesNotThrow(() -> interceptor.before(update, "cclhbot"));
        Assertions.assertFalse(TelegramSecurityUtils.isRegistered());
        Assertions.assertNotNull(TelegramContextHolder.get());
    }

    @Test
    void adminId_getsAdminRole() {
        when(roomResolverProvider.getIfAvailable()).thenReturn(null);
        when(telegramUserService.login(any())).thenReturn(telegramUser);

        new AuthUpdateInterceptor(telegramUserService, roomResolverProvider, List.of(TELEGRAM_ID))
                .before(privateMessage(), "cclhbot");

        Assertions.assertTrue(TelegramSecurityUtils.isAdmin());
    }

    /**
     * En privado no hay sala que resolver: ni se toca el resolver, que es justo lo que ahorra un
     * SELECT por update.
     */
    @Test
    void privateChat_neverResolvesRoom() {
        when(roomResolverProvider.getIfAvailable()).thenReturn(roomResolver);
        when(telegramUserService.login(any())).thenReturn(telegramUser);

        interceptor.before(privateMessage(), "cclhbot");

        Assertions.assertNull(TelegramSecurityUtils.getRoom());
        verify(roomResolver, never()).resolveRoom(anyLong(), anyString());
    }

    /**
     * En grupo sí, pero solo cuando alguien la pide, y una única vez por update.
     */
    @Test
    void groupChat_resolvesRoomLazilyAndOnlyOnce() {
        Room room = new Room();
        room.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        room.setRoomname("tg:" + GROUP_CHAT_ID);
        room.setName("Grupo de pruebas");

        when(roomResolverProvider.getIfAvailable()).thenReturn(roomResolver);
        when(telegramUserService.login(any())).thenReturn(telegramUser);
        when(roomResolver.resolveRoom(GROUP_CHAT_ID, "Grupo de pruebas")).thenReturn(room);

        interceptor.before(groupMessage(), "cclhbot");

        verify(roomResolver, never()).resolveRoom(anyLong(), anyString());

        Assertions.assertEquals(room, TelegramSecurityUtils.getRoom());
        Assertions.assertEquals(room, TelegramSecurityUtils.getRoom());
        verify(roomResolver).resolveRoom(GROUP_CHAT_ID, "Grupo de pruebas");
    }

    private Update privateMessage() {
        return messageUpdate(TELEGRAM_ID, "private", null);
    }

    private Update groupMessage() {
        return messageUpdate(GROUP_CHAT_ID, "group", "Grupo de pruebas");
    }

    private Update messageUpdate(long chatId, String chatType, String chatTitle) {
        org.telegram.telegrambots.meta.api.objects.User from = org.telegram.telegrambots.meta.api.objects.User.builder()
                .id(TELEGRAM_ID)
                .firstName("Mario")
                .isBot(false)
                .userName("themarioga")
                .languageCode("es")
                .build();

        Chat chat = Chat.builder().id(chatId).type(chatType).title(chatTitle).build();

        Message message = Message.builder().messageId(42).from(from).chat(chat).text("/start").build();

        Update update = new Update();
        update.setMessage(message);

        return update;
    }

}
