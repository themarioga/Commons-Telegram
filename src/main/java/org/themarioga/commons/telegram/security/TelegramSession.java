package org.themarioga.commons.telegram.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Copia de la sesión en curso, para poder continuar el trabajo en otro hilo.
 * <p>
 * Hace falta porque los envíos asíncronos ejecutan su continuación en un hilo del pool de OkHttp,
 * donde no hay ni contexto de seguridad ni contexto de Telegram: el motor no encontraría al usuario
 * y el bot no sabría a qué chat responder.
 * <p>
 * El código anterior resolvía la mitad del problema —rehacía el {@code UserDetails} a mano dentro
 * del callback— pero no lo limpiaba después, así que la sesión se quedaba pegada al hilo del pool y
 * se la encontraba el siguiente trabajo que cayera ahí. {@link #run(Runnable)} limpia siempre.
 */
public final class TelegramSession {

    private final Authentication authentication;
    private final TelegramContext context;

    private TelegramSession(Authentication authentication, TelegramContext context) {
        this.authentication = authentication;
        this.context = context;
    }

    /**
     * Toma la sesión del hilo actual. Hay que llamarlo <b>antes</b> de lanzar el trabajo asíncrono,
     * estando todavía en el hilo que atiende el update.
     */
    public static TelegramSession capture() {
        return new TelegramSession(SecurityContextHolder.getContext().getAuthentication(),
                TelegramContextHolder.get());
    }

    /**
     * Ejecuta la acción con esta sesión puesta y deja el hilo como estaba, pase lo que pase.
     * <p>
     * Restaura lo que hubiera en vez de limpiar sin más: normalmente esto corre en un hilo del pool
     * que no tenía nada, pero si alguna vez se ejecuta en el mismo hilo que atiende el update
     * (porque el envío se resolvió sin salir del hilo), limpiar le quitaría la sesión al resto del
     * update.
     */
    public void run(Runnable action) {
        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        TelegramContext previousContext = TelegramContextHolder.get();

        SecurityContextHolder.getContext().setAuthentication(authentication);
        TelegramContextHolder.set(context);

        try {
            action.run();
        } finally {
            if (previousAuthentication == null) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
            }

            if (previousContext == null) {
                TelegramContextHolder.clear();
            } else {
                TelegramContextHolder.set(previousContext);
            }
        }
    }

}
