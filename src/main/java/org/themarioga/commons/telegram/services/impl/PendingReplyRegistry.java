package org.themarioga.commons.telegram.services.impl;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comandos que están esperando a que el usuario conteste al mensaje anterior.
 * <p>
 * Vive fuera del bot a propósito. Cuando esto era un {@code Map} dentro de {@code BotService}, la
 * lógica de juego necesitaba el bot para dejar una respuesta pendiente, el bot necesitaba el
 * {@code ApplicationService} para conocer sus comandos y el {@code ApplicationService} necesitaba la
 * lógica de juego: un ciclo de dependencias por constructor que Spring no puede resolver (y que
 * {@code spring.main.allow-circular-references=true} tampoco arreglaba, porque esa opción solo actúa
 * sobre inyección por campo o por setter).
 * <p>
 * La clave incluye el nombre del bot para que los dos bots del despliegue no se pisen.
 */
@Component
public class PendingReplyRegistry {

    private final Map<String, String> pendingReplies = new ConcurrentHashMap<>();

    /**
     * @param chatId id del <b>chat</b>, no del usuario: es por lo que se pregunta al llegar la
     *               respuesta, y en un grupo no coinciden.
     */
    public void set(String botName, Long chatId, String command) {
        pendingReplies.put(key(botName, chatId), command);
    }

    /**
     * Devuelve el comando pendiente y lo consume.
     */
    public String poll(String botName, Long chatId) {
        return pendingReplies.remove(key(botName, chatId));
    }

    public boolean has(String botName, Long chatId) {
        return pendingReplies.containsKey(key(botName, chatId));
    }

    public void clear(String botName, Long chatId) {
        pendingReplies.remove(key(botName, chatId));
    }

    private String key(String botName, Long chatId) {
        return botName + ':' + chatId;
    }

}
