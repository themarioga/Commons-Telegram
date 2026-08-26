package org.themarioga.commons.telegram.security;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.themarioga.commons.engine.models.Room;
import org.themarioga.commons.telegram.constants.BotConstants;
import org.themarioga.commons.telegram.services.intf.TelegramRoomResolver;

/**
 * Datos del update que se está atendiendo. Vive lo que dura la petición y no forma parte de la
 * identidad, por eso va aquí y no dentro de {@link TelegramUserDetails}.
 * <p>
 * La sala se resuelve <b>de forma perezosa</b>: la mayoría de los updates llegan por chat privado y
 * no tienen sala, así que resolverla siempre sería un SELECT tirado a la basura en cada mensaje.
 */
public class TelegramContext {

    private final String botName;
    private final User from;
    private final Long chatId;
    private final String chatType;
    private final String chatTitle;
    private final Integer messageId;
    private final String callbackQueryId;
    private final TelegramRoomResolver roomResolver;

    private Room room;
    private boolean roomResolved;

    private TelegramContext(String botName, User from, Chat chat, Integer messageId, String callbackQueryId,
                            TelegramRoomResolver roomResolver) {
        this.botName = botName;
        this.from = from;
        this.chatId = chat != null ? chat.getId() : null;
        this.chatType = chat != null ? chat.getType() : null;
        this.chatTitle = chat != null ? chat.getTitle() : null;
        this.messageId = messageId;
        this.callbackQueryId = callbackQueryId;
        this.roomResolver = roomResolver;
    }

    public static TelegramContext from(Update update, String botName, TelegramRoomResolver roomResolver) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            return new TelegramContext(botName, message.getFrom(), message.getChat(), message.getMessageId(), null, roomResolver);
        }

        if (update.hasCallbackQuery()) {
            org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery = update.getCallbackQuery();
            Message message = callbackQuery.getMessage() instanceof Message m ? m : null;

            return new TelegramContext(botName, callbackQuery.getFrom(), message != null ? message.getChat() : null,
                    message != null ? message.getMessageId() : null, callbackQuery.getId(), roomResolver);
        }

        return new TelegramContext(botName, null, null, null, null, roomResolver);
    }

    public String getBotName() {
        return botName;
    }

    public User getFrom() {
        return from;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getChatType() {
        return chatType;
    }

    public String getChatTitle() {
        return chatTitle;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public String getCallbackQueryId() {
        return callbackQueryId;
    }

    public boolean isPrivate() {
        return BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE.equals(chatType);
    }

    /**
     * Sala del motor correspondiente a este chat, o {@code null} si el chat es privado o no hay
     * ningún juego que sepa traducir chats a salas. Se resuelve una sola vez por petición.
     */
    public Room getRoom() {
        if (roomResolved) return room;

        roomResolved = true;
        if (chatId != null && !isPrivate() && roomResolver != null) {
            room = roomResolver.resolveRoom(chatId, chatTitle);
        }

        return room;
    }

}
