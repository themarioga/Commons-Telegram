package org.themarioga.telegram.commons.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.webhook.starter.TelegramBotsSpringWebhookApplication;

/**
 * Endpoint al que Telegram entrega los updates en modo webhook.
 * <p>
 * El path coincide con el {@code botPath} con el que se registró cada bot, de forma que un mismo
 * despliegue puede atender a varios bots.
 */
@RestController
@RequestMapping("/callback")
@ConditionalOnProperty(prefix = "telegram.bots", name = "type", havingValue = "webhook")
public class TelegramWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramBotsSpringWebhookApplication application;

    public TelegramWebhookController(TelegramBotsSpringWebhookApplication application) {
        this.application = application;
    }

    @PostMapping("/{botPath}")
    public ResponseEntity<BotApiMethod<?>> receiveUpdate(@PathVariable String botPath, @RequestBody Update update) {
        try {
            return ResponseEntity.ok(application.receiveUpdate(botPath, update));
        } catch (Exception e) {
            // Devolver un error haría que Telegram reintentara el update indefinidamente.
            logger.error("Error atendiendo el update del bot {}: {}", botPath, e.getMessage(), e);

            return ResponseEntity.ok().build();
        }
    }

}
