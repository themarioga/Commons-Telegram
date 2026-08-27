package org.themarioga.commons.telegram.services.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Recuerda la última lista que se le enseñó a cada chat, para poder ofrecerle números cortos en vez
 * de identificadores.
 * <p>
 * Los identificadores del motor son UUID de 36 caracteres. Pedirle al usuario que conteste con uno
 * es pedirle que copie y pegue; con esto contesta "3".
 * <p>
 * Se guarda la lista concreta que vio, y no se recalcula al resolver, a propósito: entre que el bot
 * enseña la lista y el usuario contesta, otro colaborador puede haber añadido o borrado algo, y
 * recalcular haría que el número 3 señalara a otra cosa. En un flujo de borrado eso es grave.
 */
@Component
public class SelectionRegistry {

    private final Map<String, List<UUID>> selections = new ConcurrentHashMap<>();

    public void remember(String botName, Long chatId, List<UUID> ids) {
        selections.put(key(botName, chatId), List.copyOf(ids));
    }

    /**
     * Traduce lo que ha contestado el usuario al identificador correspondiente.
     *
     * @param input el número de la lista (empezando en 1) o un identificador completo
     * @return el identificador, o {@code null} si no se reconoce
     */
    public UUID resolve(String botName, Long chatId, String input) {
        if (input == null || input.isBlank()) return null;

        String trimmed = input.trim();

        List<UUID> remembered = selections.get(key(botName, chatId));
        if (remembered != null) {
            try {
                int index = Integer.parseInt(trimmed);
                if (index >= 1 && index <= remembered.size()) return remembered.get(index - 1);
            } catch (NumberFormatException e) {
                // No es un número: se intenta como identificador
            }
        }

        // Se sigue aceptando el identificador completo: es lo que llevan los botones
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void clear(String botName, Long chatId) {
        selections.remove(key(botName, chatId));
    }

    private String key(String botName, Long chatId) {
        return botName + ':' + chatId;
    }

}
