package org.themarioga.telegram.commons.services.intf;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.themarioga.telegram.commons.models.CallbackQueryHandler;
import org.themarioga.telegram.commons.models.CommandHandler;

import java.util.Map;

public interface BotService {

    String getBotToken();

    String getBotName();

    Map<String, CallbackQueryHandler> getCallbackQueries();

    Map<String, CommandHandler> getCommands();

    TelegramClient getTelegramClient();

    /**
     * El bean que espera la infraestructura de telegrambots: {@code SpringLongPollingBot} o
     * {@code SpringTelegramWebhookBot} según el modo.
     */
    Object getBean();

}
