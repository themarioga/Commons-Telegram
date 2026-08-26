package org.themarioga.telegram.commons.models;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackQueryHandler {

    void callback(CallbackQuery callbackQuery, String params);

}
