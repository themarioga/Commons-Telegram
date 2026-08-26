package org.themarioga.commons.telegram.dao.intf;

import org.themarioga.commons.engine.dao.InterfaceHibernateDao;
import org.themarioga.commons.engine.models.User;
import org.themarioga.commons.telegram.models.TelegramUser;

public interface TelegramUserDao extends InterfaceHibernateDao<TelegramUser> {

    /**
     * Carga la equivalencia trayéndose el usuario y su idioma en la misma consulta.
     * <p>
     * El {@code JOIN FETCH} no es opcional: el {@code User} viaja dentro del principal de seguridad
     * y se lee fuera de toda transacción (por ejemplo desde {@code I18NService.get(tag)}, que llama a
     * {@code SecurityUtils.getLang()}), donde una relación perezosa reventaría con
     * {@code LazyInitializationException}.
     */
    TelegramUser getByIdFetchingUser(Long telegramId);

    TelegramUser getByUser(User user);

    /**
     * Fuerza el volcado de los cambios pendientes. Necesario al reasignar un alias entre usuarios:
     * el índice único de {@code Users.username} obliga a liberar el alias antes de asignarlo.
     */
    void flush();

}
