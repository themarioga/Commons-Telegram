package org.themarioga.commons.telegram.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.themarioga.commons.engine.models.Lang;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.engine.security.SecurityUtils;
import org.themarioga.commons.engine.security.UserRole;
import org.themarioga.commons.telegram.security.TelegramContextHolder;
import org.themarioga.commons.telegram.security.TelegramSession;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Los envíos asíncronos continúan en un hilo del pool, donde no hay sesión. Esto comprueba que se
 * puede llevar, y —sobre todo— que se retira al terminar.
 */
class TelegramSessionTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TelegramContextHolder.clear();
    }

    private User givenLoggedInUser() {
        Lang lang = new Lang();
        lang.setId("es");
        lang.setName("Español");

        User user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        user.setUsername("themarioga");
        user.setName("Mario");
        user.setActive(true);
        user.setLang(lang);

        SecurityUtils.setUserDetails(user, UserRole.USER);

        return user;
    }

    @Test
    void carriesTheSessionToAnotherThread() throws Exception {
        User user = givenLoggedInUser();
        TelegramSession session = TelegramSession.capture();

        AtomicReference<UUID> seen = new AtomicReference<>();
        CompletableFuture.runAsync(() -> session.run(() -> seen.set(SecurityUtils.getId()))).get();

        Assertions.assertEquals(user.getId(), seen.get());
    }

    /**
     * Lo que el código anterior no hacía: el hilo del pool se reutiliza, así que dejarle la sesión
     * puesta significa que el siguiente trabajo que caiga ahí la hereda.
     */
    @Test
    void leavesNothingBehindOnThatThread() throws Exception {
        givenLoggedInUser();
        TelegramSession session = TelegramSession.capture();

        AtomicReference<Object> afterwards = new AtomicReference<>();
        CompletableFuture.runAsync(() -> {
            session.run(() -> {
                /* el trabajo asíncrono */ });

            afterwards.set(SecurityUtils.getUserDetails());
        }).get();

        Assertions.assertNull(afterwards.get(), "la sesión se ha quedado pegada al hilo del pool");
    }

    @Test
    void cleansUpEvenIfTheWorkFails() throws Exception {
        givenLoggedInUser();
        TelegramSession session = TelegramSession.capture();

        AtomicReference<Object> afterwards = new AtomicReference<>();
        CompletableFuture.runAsync(() -> {
            try {
                session.run(() -> {
                    throw new IllegalStateException("boom");
                });
            } catch (IllegalStateException expected) {
                // se propaga, pero la sesión tiene que quedar limpia igual
            }

            afterwards.set(SecurityUtils.getUserDetails());
        }).get();

        Assertions.assertNull(afterwards.get());
    }

    @Test
    void capturingWithoutSessionIsHarmless() throws Exception {
        TelegramSession session = TelegramSession.capture();

        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        CompletableFuture.runAsync(() -> session.run(() -> ran.set(true))).get();

        Assertions.assertTrue(ran.get());
    }

}
