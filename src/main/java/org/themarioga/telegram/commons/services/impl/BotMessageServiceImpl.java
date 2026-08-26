package org.themarioga.telegram.commons.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.themarioga.telegram.commons.services.intf.BotMessageService;

public class BotMessageServiceImpl implements BotMessageService {

    private static final Logger logger = LoggerFactory.getLogger(BotMessageServiceImpl.class);

    private final TelegramClient telegramClient;
    private final String botName;

    // Depende del cliente y del nombre, no del BotService: pedir el bot entero creaba un ciclo de
    // dependencias con el ApplicationService.
    public BotMessageServiceImpl(TelegramClient telegramClient, String botName) {
        this.telegramClient = telegramClient;
        this.botName = botName;
    }

    @Override
    public void sendMessage(long chatId, String text) {
        try {
            SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
            sendMessage.enableHtml(true);
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void sendMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        try {
            SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void sendMessageAsync(long chatId, String text, Callback callback) {
        try {
            SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
            sendMessage.enableHtml(true);
            telegramClient.executeAsync(sendMessage).thenAccept(callback::success).exceptionally(throwable -> {
                callback.failure(throwable);
                return null;
            });
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void editMessage(long chatId, int messageId, String text) {
        try {
            EditMessageText editMessageText = EditMessageText.builder().chatId(chatId).messageId(messageId).text(text).parseMode("HTML").build();
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void editMessage(long chatId, int messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        try {
            EditMessageText editMessageText = EditMessageText.builder().chatId(chatId).messageId(messageId).text(text).parseMode("HTML").replyMarkup(inlineKeyboardMarkup).build();
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        try {
            telegramClient.execute(new DeleteMessage(String.valueOf(chatId), messageId));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        try {
            telegramClient.execute(new AnswerCallbackQuery(callbackQueryId));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQueryId);
            answerCallbackQuery.setText(text);
            telegramClient.execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String sanitizeTextFromCommand(String command, String text) {
        return text.replace(command, "").replace("@" + botName, "").trim();
    }

}
