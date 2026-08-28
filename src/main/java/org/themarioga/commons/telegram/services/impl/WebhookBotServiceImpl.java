package org.themarioga.commons.telegram.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.starter.SpringTelegramWebhookBot;
import org.themarioga.commons.telegram.models.CallbackQueryHandler;
import org.themarioga.commons.telegram.models.CommandHandler;
import org.themarioga.commons.telegram.models.UpdateInterceptor;
import org.themarioga.commons.telegram.services.intf.ApplicationService;
import org.themarioga.commons.telegram.services.intf.BotService;
import org.themarioga.commons.telegram.util.BotCreationUtils;

import java.util.List;
import java.util.Map;

public class WebhookBotServiceImpl implements BotService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookBotServiceImpl.class);

    private final String botToken;
    private final String botName;

    private final TelegramClient telegramClient;

    private final Map<String, CommandHandler> commands;
    private final Map<String, CallbackQueryHandler> callbackQueries;
    private final UpdateDispatcher updateDispatcher;

    private final SpringTelegramWebhookBot springTelegramWebhookBot;

    public WebhookBotServiceImpl(String botToken, String botName, String webhookURL, String webhookCertPath, TelegramClient telegramClient, ApplicationService applicationService, PendingReplyRegistry pendingReplies, List<UpdateInterceptor> interceptors) {
        logger.info("Iniciando {} webhook en la url {}...", botName, webhookURL);

        this.botToken = botToken;
        this.botName = botName;
        this.telegramClient = telegramClient;

        commands = applicationService.getBotCommands();
        callbackQueries = applicationService.getCallbackQueries();

        updateDispatcher = new UpdateDispatcher(botName, telegramClient, commands, callbackQueries, pendingReplies, interceptors);

        SpringTelegramWebhookBot.SpringTelegramWebhookBotBuilder botBuilder = SpringTelegramWebhookBot.builder();
        // Sin barra inicial: es la variable de ruta de TelegramWebhookController (/callback/{botPath}),
        // y con ella la URL que hay que declarar en Telegram es <host>/callback/<botName>.
        botBuilder.botPath(botName);
        botBuilder.setWebhook(() -> BotCreationUtils.setWebhook(webhookURL, webhookCertPath, telegramClient));
        botBuilder.deleteWebhook(() -> BotCreationUtils.deleteWebhook(telegramClient));
        botBuilder.updateHandler(this::onWebhookUpdateReceived);

        springTelegramWebhookBot = botBuilder.build();
    }

    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        updateDispatcher.dispatch(update);

        return null;
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
    public SpringTelegramWebhookBot getBean() {
        return springTelegramWebhookBot;
    }

}
