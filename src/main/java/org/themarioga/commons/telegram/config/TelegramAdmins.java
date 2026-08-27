package org.themarioga.commons.telegram.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Ids de Telegram con permisos de administración, de {@code telegram.bots.admin-ids}.
 * <p>
 * Lo consultan tanto el interceptor que asigna el rol de la sesión como los flujos que tienen que
 * escribirle a quien administra el bot, así que la propiedad se lee en un único sitio.
 */
@Component
public class TelegramAdmins {

    private final List<Long> ids;
    private final Set<Long> index;

    public TelegramAdmins(@Value("${telegram.bots.admin-ids:}") List<Long> ids) {
        this.ids = ids != null ? List.copyOf(ids) : List.of();
        this.index = Set.copyOf(this.ids);
    }

    public boolean contains(Long telegramId) {
        return index.contains(telegramId);
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /**
     * @return el primero de la lista, o {@code null} si no hay ninguno configurado.
     */
    public Long first() {
        return ids.isEmpty() ? null : ids.get(0);
    }

    public List<Long> getIds() {
        return ids;
    }

}
