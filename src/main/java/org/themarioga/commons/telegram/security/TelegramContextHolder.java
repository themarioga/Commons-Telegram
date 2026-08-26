package org.themarioga.commons.telegram.security;

/**
 * Guarda el {@link TelegramContext} de la petición en curso.
 * <p>
 * Es un {@code ThreadLocal} y <b>hay que limpiarlo siempre</b>: en long-polling el hilo se reutiliza
 * para todos los updates y en webhook los hilos de Tomcat se reciclan entre peticiones, así que un
 * contexto sin limpiar lo hereda el siguiente usuario.
 */
public class TelegramContextHolder {

    private static final ThreadLocal<TelegramContext> CONTEXT = new ThreadLocal<>();

    private TelegramContextHolder() {
        throw new UnsupportedOperationException();
    }

    public static void set(TelegramContext context) {
        CONTEXT.set(context);
    }

    public static TelegramContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

}
