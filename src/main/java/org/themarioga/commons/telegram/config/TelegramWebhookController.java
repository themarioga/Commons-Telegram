package org.themarioga.commons.telegram.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
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
 * <p>
 * El cuerpo se recibe como texto plano y se convierte a mano con Jackson 2: los objetos de
 * telegrambots están anotados con {@code @Jacksonized}, que genera un
 * {@code com.fasterxml.jackson.databind.annotation.@JsonDeserialize(builder = ...)}. Spring Boot 4
 * convierte con Jackson 3, cuyo {@code @JsonDeserialize} vive en {@code tools.jackson.databind},
 * así que ignora esa anotación, no encuentra constructor utilizable en clases sin constructor vacío
 * ({@code MessageEntity}, {@code User}, ...) y revienta con {@code InvalidDefinitionException}.
 * Es el mismo {@link ObjectMapper} que usa la librería en modo long polling.
 */
@RestController
@RequestMapping("/callback")
@ConditionalOnProperty(prefix = "telegram.bots", name = "type", havingValue = "webhook")
public class TelegramWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final TelegramBotsSpringWebhookApplication application;

    public TelegramWebhookController(TelegramBotsSpringWebhookApplication application) {
        this.application = application;
    }

    @PostMapping(value = "/{botPath}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiveUpdate(@PathVariable String botPath, @RequestBody String body) {
        try {
            Update update = OBJECT_MAPPER.readValue(body, Update.class);

            BotApiMethod<?> response = application.receiveUpdate(botPath, update);

            return response != null ? ResponseEntity.ok(OBJECT_MAPPER.writeValueAsString(response)) : ResponseEntity.ok().build();
        } catch (Exception e) {
            // Devolver un error haría que Telegram reintentara el update indefinidamente.
            logger.error("Error atendiendo el update del bot {}: {}", botPath, e.getMessage(), e);

            return ResponseEntity.ok().build();
        }
    }

}
