package org.themarioga.telegram.commons.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.themarioga.telegram.commons.constants.BotResponseErrorI18n;
import org.themarioga.telegram.commons.models.CallbackQuery;
import org.themarioga.telegram.commons.models.CallbackQueryHandler;
import org.themarioga.telegram.commons.models.Command;
import org.themarioga.telegram.commons.models.CommandHandler;
import org.themarioga.telegram.commons.models.UpdateInterceptor;
import org.themarioga.telegram.commons.util.BotMessageUtils;

import java.util.List;
import java.util.Map;

/**
 * Reparte cada update a su handler, rodeado de los interceptores configurados.
 * <p>
 * Lo usan por igual el bot de long-polling y el de webhook, que antes tenían este bucle duplicado
 * línea por línea.
 */
public class UpdateDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final String botName;
    private final TelegramClient telegramClient;
    private final Map<String, CommandHandler> commands;
    private final Map<String, CallbackQueryHandler> callbackQueries;
    private final PendingReplyRegistry pendingReplies;
    private final List<UpdateInterceptor> interceptors;

    public UpdateDispatcher(String botName, TelegramClient telegramClient, Map<String, CommandHandler> commands,
                            Map<String, CallbackQueryHandler> callbackQueries, PendingReplyRegistry pendingReplies,
                            List<UpdateInterceptor> interceptors) {
        this.botName = botName;
        this.telegramClient = telegramClient;
        this.commands = commands;
        this.callbackQueries = callbackQueries;
        this.pendingReplies = pendingReplies;
        this.interceptors = interceptors != null ? interceptors : List.of();
    }

    public void dispatch(Update update) {
        try {
            for (UpdateInterceptor interceptor : interceptors) {
                interceptor.before(update, botName);
            }

            handle(update);
        } finally {
            // En orden inverso y cada uno por su cuenta: que uno falle no puede dejar sin limpiar
            // a los demás, o el siguiente update heredaría la sesión de este.
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                try {
                    interceptors.get(i).after(update);
                } catch (Exception e) {
                    logger.error("Error al cerrar el interceptor {}: {}", interceptors.get(i).getClass(), e.getMessage(), e);
                }
            }
        }
    }

    private void handle(Update update) {
        if (update.hasMessage()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleMessage(Update update) {
        String receivedCommand = BotMessageUtils.getReceivedCommand(botName, update.getMessage(), pendingReplies);
        if (receivedCommand == null || receivedCommand.isBlank()) return;

        Command command = BotMessageUtils.getCommandFromMessage(receivedCommand);
        CommandHandler commandHandler = commands.get(command.getCommand());
        if (commandHandler != null) {
            commandHandler.callback(update.getMessage(), command.getCommandData());
        } else {
            logger.error("Comando desconocido {} enviado por {}", update.getMessage().getText(),
                    BotMessageUtils.getUserInfo(update.getMessage().getFrom()));

            try {
                telegramClient.execute(new SendMessage(String.valueOf(update.getMessage().getChatId()),
                        BotResponseErrorI18n.COMMAND_DOES_NOT_EXISTS));
            } catch (TelegramApiException e) {
                logger.error("Error al enviar mensaje {}", e.getMessage(), e);
            }
        }
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = BotMessageUtils.getCallbackQueryFromMessageQuery(update.getCallbackQuery().getData());

        CallbackQueryHandler callbackQueryHandler = callbackQueries.get(callbackQuery.getQuery());
        if (callbackQueryHandler != null) {
            callbackQueryHandler.callback(update.getCallbackQuery(), callbackQuery.getQueryData());
        } else {
            logger.error("Querie desconocida {} enviado por {}", update.getCallbackQuery().getData(),
                    BotMessageUtils.getUserInfo(update.getCallbackQuery().getFrom()));

            try {
                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(update.getCallbackQuery().getId());
                answerCallbackQuery.setText(BotResponseErrorI18n.COMMAND_DOES_NOT_EXISTS);
                telegramClient.execute(answerCallbackQuery);
            } catch (TelegramApiException e) {
                logger.error("Error al enviar mensaje {}", e.getMessage(), e);
            }
        }
    }

}
