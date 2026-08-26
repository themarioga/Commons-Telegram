package org.themarioga.telegram.commons.util;

import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.themarioga.telegram.commons.constants.BotConstants;
import org.themarioga.telegram.commons.models.CallbackQuery;
import org.themarioga.telegram.commons.models.Command;
import org.themarioga.telegram.commons.services.impl.PendingReplyRegistry;

import java.util.Arrays;

public class BotMessageUtils {

    private BotMessageUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isMessagePrivate(Message message) {
        return message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE);
    }

    public static String getReceivedCommand(String botUsername, Message message, PendingReplyRegistry pendingReplies) {
        if (message.getText() != null && message.getText().startsWith("/")) {
            return message.getText().replace("@" + botUsername, "");
        }

        if (message.isReply()) {
            return pendingReplies.poll(botUsername, message.getChatId());
        }

        return null;
    }

    public static Command getCommandFromMessage(String message) {
        String[] receivedMessage = message.split(" ")[0].split("__");

        Command command = new Command();
        command.setCommand(receivedMessage[0]);
        command.setCommandData(receivedMessage.length > 1 ? receivedMessage[1] : null);

        return command;
    }

    public static CallbackQuery getCallbackQueryFromMessageQuery(String query) {
        String[] receivedQuery = query.split("__");

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setQuery(receivedQuery[0]);
        callbackQuery.setQueryData(receivedQuery.length > 1 ? receivedQuery[1] : null);

        return callbackQuery;
    }

    public static String getUserInfo(User user) {
        String output = String.valueOf(user.getId());
        if (StringUtils.hasText(user.getFirstName()) || StringUtils.hasText(user.getLastName()) || StringUtils.hasText(user.getUserName())) {
            output += " [" + getUsername(user) + "]";
        }

        return output;
    }

    public static String getUsername(User user) {
        String output = "";
        if (StringUtils.hasText(user.getFirstName())) output += user.getFirstName();
        if (StringUtils.hasText(user.getLastName())) output += " " + user.getLastName();
        if (StringUtils.hasText(user.getUserName())) output += " (@" + user.getUserName() + ")";
        return output;
    }

    public static String getUsername(Chat chat) {
        String output = "";
        if (StringUtils.hasText(chat.getFirstName())) output += chat.getFirstName();
        if (StringUtils.hasText(chat.getLastName())) output += " " + chat.getLastName();
        if (StringUtils.hasText(chat.getUserName())) output += " (@" + chat.getUserName() + ")";
        return output;
    }

    public static String arrayToMessage(String[] array) {
        return String.join(" ", Arrays.copyOfRange(array, 1, array.length));
    }

}
