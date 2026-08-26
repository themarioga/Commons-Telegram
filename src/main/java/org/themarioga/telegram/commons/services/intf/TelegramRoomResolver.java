package org.themarioga.telegram.commons.services.intf;

import org.themarioga.engine.commons.models.Room;

/**
 * Traduce un chat de Telegram a una sala del motor, creándola junto a su equivalencia la primera vez
 * que se juega en ese grupo.
 * <p>
 * La implementa cada juego (cada uno tiene su propia tabla de equivalencias), lo que permite que
 * {@link org.themarioga.telegram.commons.security.TelegramSecurityUtils#getRoom()} resuelva la sala
 * de forma perezosa sin que esta librería sepa nada de CAH ni de SH.
 */
public interface TelegramRoomResolver {

    Room resolveRoom(long chatId, String title);

}
