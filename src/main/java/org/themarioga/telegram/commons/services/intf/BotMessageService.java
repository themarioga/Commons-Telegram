package org.themarioga.telegram.commons.services.intf;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface BotMessageService {

    void sendMessage(long chatId, String text);

    void sendMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup);

    void sendMessageAsync(long chatId, String text, Callback callback);

    void editMessage(long chatId, int messageId, String text);

    void editMessage(long chatId, int messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup);

    void deleteMessage(long chatId, int messageId);

    void answerCallbackQuery(String callbackQueryId);

    void answerCallbackQuery(String callbackQueryId, String text);

    String sanitizeTextFromCommand(String command, String text);

    interface Callback {

        void success(Message response);

        void failure(Throwable e);

    }

}
