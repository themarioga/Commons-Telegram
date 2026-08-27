package org.themarioga.commons.telegram.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.themarioga.commons.telegram.services.impl.SelectionRegistry;

import java.util.List;
import java.util.UUID;

class SelectionRegistryTest {

    private static final String BOT = "dictionariesbot";
    private static final long CHAT = 123456789L;

    private static final UUID FIRST = UUID.fromString("00000000-0000-4000-a000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-4000-a000-000000000002");
    private static final UUID THIRD = UUID.fromString("00000000-0000-4000-a000-000000000003");

    private SelectionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SelectionRegistry();
        registry.remember(BOT, CHAT, List.of(FIRST, SECOND, THIRD));
    }

    @Test
    void resolvesTheShortNumberTheUserSees() {
        Assertions.assertEquals(FIRST, registry.resolve(BOT, CHAT, "1"));
        Assertions.assertEquals(THIRD, registry.resolve(BOT, CHAT, "3"));
        Assertions.assertEquals(SECOND, registry.resolve(BOT, CHAT, "  2  "), "con espacios alrededor");
    }

    /**
     * Los botones siguen llevando el identificador completo, así que hay que aceptarlo igual.
     */
    @Test
    void stillAcceptsAFullIdentifier() {
        Assertions.assertEquals(SECOND, registry.resolve(BOT, CHAT, SECOND.toString()));
    }

    @Test
    void rejectsWhatIsNotInTheList() {
        Assertions.assertNull(registry.resolve(BOT, CHAT, "0"));
        Assertions.assertNull(registry.resolve(BOT, CHAT, "4"));
        Assertions.assertNull(registry.resolve(BOT, CHAT, "-1"));
        Assertions.assertNull(registry.resolve(BOT, CHAT, "pepe"));
        Assertions.assertNull(registry.resolve(BOT, CHAT, ""));
        Assertions.assertNull(registry.resolve(BOT, CHAT, null));
    }

    /**
     * Cada chat tiene su lista: el número 1 de uno no es el del otro.
     */
    @Test
    void listsAreIsolatedPerChatAndBot() {
        registry.remember(BOT, 999L, List.of(THIRD));

        Assertions.assertEquals(FIRST, registry.resolve(BOT, CHAT, "1"));
        Assertions.assertEquals(THIRD, registry.resolve(BOT, 999L, "1"));
        Assertions.assertNull(registry.resolve("otrobot", CHAT, "1"));
    }

    /**
     * Se guarda la lista que el usuario vio, no se recalcula: si mientras contestaba cambió el
     * contenido, el número 2 tiene que seguir señalando a lo que él tenía delante.
     */
    @Test
    void keepsWhatTheUserSawEvenIfThingsChange() {
        UUID chosen = registry.resolve(BOT, CHAT, "2");

        registry.remember(BOT, CHAT, List.of(THIRD, FIRST));

        Assertions.assertEquals(SECOND, chosen);
        Assertions.assertEquals(THIRD, registry.resolve(BOT, CHAT, "1"), "la lista nueva manda a partir de ahora");
    }

}
