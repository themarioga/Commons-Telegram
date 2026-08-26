package org.themarioga.commons.telegram.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.webhook.starter.SpringTelegramWebhookBot;
import org.telegram.telegrambots.webhook.starter.TelegramBotsSpringWebhookApplication;

import java.util.List;

/**
 * Arranca los bots registrados en el contexto, en long-polling o en webhook según
 * {@code telegram.bots.type}.
 * <p>
 * Sustituye a las autoconfiguraciones de {@code telegrambots-springboot-longpolling-starter} y
 * {@code telegrambots-springboot-webhook-starter}: <b>las dos declaran un bean llamado
 * {@code telegramBotsApplication}</b> y, como Spring Boot 4 no permite sobrescribir definiciones de
 * beans, tenerlas ambas en el classpath impide que la aplicación arranque. Las dos hay que
 * excluirlas con {@code spring.autoconfigure.exclude}.
 * <p>
 * No se pierde nada por hacerlo aquí: los starters solo creaban el objeto de aplicación y le
 * registraban los bots, que es exactamente lo que hace esta clase con nombres de bean distintos.
 */
@Configuration
public class TelegramBotsRegistrarConfig {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotsRegistrarConfig.class);

    @Configuration
    @ConditionalOnProperty(prefix = "telegram.bots", name = "type", havingValue = "longpolling", matchIfMissing = true)
    public static class LongPollingConfig {

        @Bean(destroyMethod = "close")
        public TelegramBotsLongPollingApplication telegramLongPollingApplication(
                ObjectProvider<List<SpringLongPollingBot>> bots) {
            TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();

            for (SpringLongPollingBot bot : bots.getIfAvailable(List::of)) {
                // registerBot llama a Telegram para validar el token. Si un bot falla (token mal, o
                // Telegram caído al arrancar) se registra el error y se sigue con los demás: tumbar
                // todo el despliegue dejaría también sin servicio al bot que sí funciona.
                try {
                    application.registerBot(bot.getBotToken(), bot.getUpdatesConsumer());

                    logger.info("Bot registrado en long-polling: {}", bot.getClass().getSimpleName());
                } catch (TelegramApiException e) {
                    logger.error("No se ha podido registrar el bot {} en long-polling: {}",
                            bot.getClass().getSimpleName(), e.getMessage(), e);
                }
            }

            return application;
        }

    }

    /**
     * Ojo: el starter de webhook <b>no publica ningún endpoint HTTP</b>, solo expone un
     * {@code receiveUpdate(path, update)} que alguien tiene que llamar. Quien lo hace es
     * {@link TelegramWebhookController}, que se activa con esta misma propiedad.
     */
    @Configuration
    @ConditionalOnProperty(prefix = "telegram.bots", name = "type", havingValue = "webhook")
    public static class WebhookConfig {

        @Bean(destroyMethod = "close")
        public TelegramBotsSpringWebhookApplication telegramWebhookApplication(
                ObjectProvider<List<SpringTelegramWebhookBot>> bots) throws TelegramApiException {
            TelegramBotsSpringWebhookApplication application = new TelegramBotsSpringWebhookApplication();

            for (SpringTelegramWebhookBot bot : bots.getIfAvailable(List::of)) {
                // Igual que en long-polling: registrar declara el webhook contra Telegram, así que
                // un bot que falle no puede impedir que arranque el resto.
                try {
                    application.registerBot(bot);

                    logger.info("Bot registrado en webhook con path {}", bot.getBotPath());
                } catch (TelegramApiException e) {
                    logger.error("No se ha podido registrar el bot con path {}: {}", bot.getBotPath(), e.getMessage(), e);
                }
            }

            return application;
        }

    }

}
