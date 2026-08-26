package org.themarioga.commons.telegram.services.intf;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.themarioga.commons.telegram.models.CallbackQueryHandler;
import org.themarioga.commons.telegram.models.CommandHandler;

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
