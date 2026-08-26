package org.themarioga.telegram.commons.models;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Se ejecuta alrededor de cada update, antes y después de que lo atienda su handler.
 * <p>
 * {@link #after(Update)} se invoca <b>siempre</b>, incluso si {@link #before} o el propio handler
 * revientan: es donde se limpia el estado de la petición.
 */
public interface UpdateInterceptor {

    void before(Update update, String botName);

    void after(Update update);

}
