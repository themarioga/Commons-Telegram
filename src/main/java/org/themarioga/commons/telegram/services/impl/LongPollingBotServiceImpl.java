package org.themarioga.commons.telegram.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.themarioga.commons.telegram.models.CallbackQueryHandler;
import org.themarioga.commons.telegram.models.CommandHandler;
import org.themarioga.commons.telegram.models.UpdateInterceptor;
import org.themarioga.commons.telegram.services.intf.ApplicationService;
import org.themarioga.commons.telegram.services.intf.BotService;

import java.util.List;
import java.util.Map;

public class LongPollingBotServiceImpl implements BotService, SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LongPollingBotServiceImpl.class);

    private final String botToken;
    private final String botName;

    private final TelegramClient telegramClient;

    private final Map<String, CommandHandler> commands;
    private final Map<String, CallbackQueryHandler> callbackQueries;
    private final UpdateDispatcher updateDispatcher;

    public LongPollingBotServiceImpl(String botToken, String botName, TelegramClient telegramClient,
                                     ApplicationService applicationService, PendingReplyRegistry pendingReplies,
                                     List<UpdateInterceptor> interceptors) {
        logger.info("Iniciando {} como longpolling...", botName);

        this.botToken = botToken;
        this.botName = botName;
        this.telegramClient = telegramClient;

        commands = applicationService.getBotCommands();
        callbackQueries = applicationService.getCallbackQueries();

        updateDispatcher = new UpdateDispatcher(botName, telegramClient, commands, callbackQueries, pendingReplies, interceptors);
    }

    @Override
    public void consume(Update update) {
        updateDispatcher.dispatch(update);
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotName() {
        return botName;
    }

    @Override
    public Map<String, CallbackQueryHandler> getCallbackQueries() {
        return callbackQueries;
    }

    @Override
    public Map<String, CommandHandler> getCommands() {
        return commands;
    }

    @Override
    public TelegramClient getTelegramClient() {
        return telegramClient;
    }

    @Override
    public SpringLongPollingBot getBean() {
        return this;
    }

}
