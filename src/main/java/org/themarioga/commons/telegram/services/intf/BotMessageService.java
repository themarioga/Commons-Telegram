package org.themarioga.commons.telegram.services.intf;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface BotMessageService {

    void sendMessage(long chatId, String text);

    void sendMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup);

    /**
     * Envía un mensaje que abre el cuadro de respuesta del usuario. Se usa junto a
     * {@link #setPendingReply(long, String)}: el bot pregunta algo y la respuesta se encamina al
     * comando que quedó pendiente.
     */
    void sendMessageWithForceReply(long chatId, String text);

    /**
     * Deja un comando esperando la siguiente respuesta que llegue en ese chat.
     */
    void setPendingReply(long chatId, String command);

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
