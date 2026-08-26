# Commons-Telegram
Utilidades para la creación de bots de telegram

Para utilizarlo debemos crear una clase de configuración como la siguiente:

    @Configuration
    public class ExampleBotConfig {

        @Value("${example.bot.token}")
        private String token;
    
        @Value("${example.bot.name}")
        private String name;
    
        @Value("${example.bot.webhook.url}")
        private String webhookURL;
    
        @Value("${example.bot.webhook.cert.path}")
        private String webhookCertPath;
    
        // Long polling instantiation
    
        @Bean("exampleService")
        @ConditionalOnProperty(prefix = "example.bot", name="type", havingValue = "longpolling")
        public BotService longPollingService(@Qualifier("exampleBotApplicationService") ApplicationService applicationService) {
            return new LongPollingBotServiceImpl(token, name, applicationService);
        }
    
        @Bean("exampleBot")
        @DependsOn({"exampleService"})
        @ConditionalOnExpression("${example.bot.enabled} and ${example.bot.type} == 'longpolling'")
        public SpringLongPollingBot longPollingBot(@Qualifier("exampleService") BotService botService) {
            return (SpringLongPollingBot) botService.getBean();
        }
    
        // Webhook instantiation
    
        @Bean("exampleService")
        @ConditionalOnProperty(prefix = "example.bot", name="type", havingValue = "webhook")
        public BotService webhookService(@Qualifier("exampleBotApplicationService") ApplicationService applicationService) {
            return new WebhookBotServiceImpl(token, name, webhookURL, webhookCertPath, applicationService);
        }
    
        @Bean("exampleBot")
        @DependsOn({"exampleService"})
        @ConditionalOnExpression("${example.bot.enabled} and ${example.bot.type} == 'webhook'")
        public SpringTelegramWebhookBot webhookBot(@Qualifier("exampleService") BotService botService) {
            return (SpringTelegramWebhookBot) botService.getBean();
        }
    
    }

`BotService.getBean()` returns the Spring-managed bean the corresponding telegrambots starter
(`telegrambots-springboot-longpolling-starter` / `telegrambots-springboot-webhook-starter`)
needs registered — `SpringLongPollingBot` or `SpringTelegramWebhookBot` respectively — which
is why it's re-exposed as its own `@Bean`. Note the webhook bot's path is always `/` +
`example.bot.name` (set internally by `WebhookBotServiceImpl`), so there's no separate
`example.bot.path` property to configure.

Y además debemos implementar la clase ApplicationService de una forma como la siguiente:

    @Service("exampleBotApplicationService")
    public class ApplicationServiceImpl implements ApplicationService {
    
        private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    
        private BotMessageService dictionariesBotMessageService;
    
        @Override
        public Map<String, CommandHandler> getBotCommands() {
            Map<String, CommandHandler> commands = new HashMap<>();
    
            commands.put("/start", (message, data) -> {
                if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                    logger.error("Comando /start enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));
    
                    dictionariesBotMessageService.sendMessage(message.getChat().getId(), BotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);
    
                    return;
                }
    
                try {
                    // Register user
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
    
            commands.put("/menu", (message, data) -> {
                if (message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                    logger.error("Comando /create enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));
    
                    dictionariesBotMessageService.sendMessage(message.getChat().getId(), BotResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP);
    
                    return;
                }
    
                try {
                    // Show menu
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
    
            commands.put("/help", (message, data) -> {
                // Show help
            });
    
            return commands;
        }
    
        @Override
        public Map<String, CallbackQueryHandler> getCallbackQueries() {
            Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();
    
            callbackQueryHandlerMap.put("example_query", (callbackQuery, data) -> {
                try {
                    // Answer query

                    return;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
    
                dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
            });
    
            return callbackQueryHandlerMap;
        }
    
        @Autowired
        public void setBotMessageService(BotMessageService dictionariesBotMessageService) {
            this.dictionariesBotMessageService = dictionariesBotMessageService;
        }

    }

Además tenemos que tener un fichero de configuración como el siguiente:

    spring.profiles.active=@spring.profiles.active@
    
    spring.main.allow-circular-references=true
    
    example.bot.type=longpolling
    
    example.bot.enabled=${EXAMPLE_BOT_ENABLED}
    example.bot.token=${EXAMPLE_BOT_TOKEN}
    example.bot.name=${EXAMPLE_BOT_NAME}
    example.bot.webhook.url=
    example.bot.webhook.cert.path=

O en caso de que queramos webhooks

    spring.profiles.active=@spring.profiles.active@

    server.port=${SERVER_PORT}
    
    spring.main.allow-circular-references=true
    
    example.bot.type=webhook
    
    example.bot.enabled=${EXAMPLE_BOT_ENABLED}
    example.bot.token=${EXAMPLE_BOT_TOKEN}
    example.bot.name=${EXAMPLE_BOT_NAME}
    example.bot.webhook.url=${EXAMPLE_BOT_WEBHOOK_URL}
    example.bot.webhook.cert.path=${EXAMPLE_BOT_WEBHOOK_CERT_PATH}    
    
    server.ssl.enabled=true
    server.ssl.key-store=${SERVER_KEYSTORE_FILE}
    server.ssl.key-store-password=${SERVER_KEYSTORE_PASSWORD}
    server.ssl.key-alias=${SERVER_KEYSTORE_ALIAS}
    server.ssl.key-store-type=PKCS12
    
    lets-encrypt-helper.domain=${LETS_ENCRYPT_DOMAIN}
    lets-encrypt-helper.contact=mailto:${LETS_ENCRYPT_EMAIL}