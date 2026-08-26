package org.themarioga.telegram.commons.models;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface CommandHandler {

    void callback(Message message, String params);

}
