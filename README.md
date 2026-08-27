# Commons-Telegram

Utilidades para la creación de bots de Telegram: identidad de quien escribe, sesión de cada update,
reparto a los handlers y envío de mensajes.

Un ejemplo completo y en funcionamiento está en `CAH-Telegram` (`config/CAHTelegramBotsConfig.java`,
que da de alta dos bots en el mismo despliegue).

## 1. Configuración de los beans del bot

El orden de dependencias es `TelegramClient → BotMessageService → ApplicationService → bot`, siempre
en un solo sentido:

    @Configuration
    public class ExampleBotConfig {

        @Bean("exampleTelegramClient")
        @ConditionalOnProperty(prefix = "example.bot", name = "enabled", havingValue = "true")
        public TelegramClient exampleTelegramClient(@Value("${example.bot.token}") String token) {
            return new OkHttpTelegramClient(token);
        }

        @Bean("exampleBotMessageService")
        @ConditionalOnProperty(prefix = "example.bot", name = "enabled", havingValue = "true")
        public BotMessageService exampleBotMessageService(
                @Qualifier("exampleTelegramClient") TelegramClient client,
                PendingReplyRegistry pendingReplies,
                @Value("${example.bot.name}") String name) {
            return new BotMessageServiceImpl(client, name, pendingReplies);
        }

        @Configuration
        @ConditionalOnProperty(prefix = "telegram.bots", name = "type",
                havingValue = "longpolling", matchIfMissing = true)
        public static class LongPollingBots {

            @Bean("exampleBot")
            @ConditionalOnProperty(prefix = "example.bot", name = "enabled", havingValue = "true")
            public SpringLongPollingBot exampleBot(
                    @Value("${example.bot.token}") String token,
                    @Value("${example.bot.name}") String name,
                    @Qualifier("exampleTelegramClient") TelegramClient client,
                    @Qualifier("exampleBotApplicationService") ApplicationService applicationService,
                    PendingReplyRegistry pendingReplies,
                    List<UpdateInterceptor> interceptors) {
                return new LongPollingBotServiceImpl(token, name, client, applicationService,
                        pendingReplies, interceptors);
            }

        }

        @Configuration
        @ConditionalOnProperty(prefix = "telegram.bots", name = "type", havingValue = "webhook")
        public static class WebhookBots {

            @Bean("exampleBot")
            @ConditionalOnProperty(prefix = "example.bot", name = "enabled", havingValue = "true")
            public SpringTelegramWebhookBot exampleBot(
                    @Value("${example.bot.token}") String token,
                    @Value("${example.bot.name}") String name,
                    @Value("${example.bot.webhook.url}") String webhookUrl,
                    @Value("${example.bot.webhook.cert.path:}") String certPath,
                    @Qualifier("exampleTelegramClient") TelegramClient client,
                    @Qualifier("exampleBotApplicationService") ApplicationService applicationService,
                    PendingReplyRegistry pendingReplies,
                    List<UpdateInterceptor> interceptors) {
                BotService botService = new WebhookBotServiceImpl(token, name, webhookUrl, certPath,
                        client, applicationService, pendingReplies, interceptors);

                return (SpringTelegramWebhookBot) botService.getBean();
            }

        }

    }

Dos detalles que no se ven en el código:

- **El modo es común a todo el despliegue** (`telegram.bots.type`), no por bot: es lo que decide qué
  objeto de aplicación crea `TelegramBotsRegistrarConfig`.
- **En Spring Boot 4 no se pueden declarar dos `@Bean` con el mismo nombre** distinguidos por
  `@Conditional` dentro de la misma clase; de ahí las dos configuraciones anidadas.

## 2. El ApplicationService

Es el punto de extensión: dos mapas, uno de comandos y otro de callbacks. **Sus claves son contrato
con lo que ya está desplegado**, porque los botones viven dentro de mensajes que Telegram guarda
indefinidamente.

    @Service("exampleBotApplicationService")
    public class ExampleApplicationServiceImpl implements ApplicationService {

        private final ExampleTelegramService exampleTelegramService;

        public ExampleApplicationServiceImpl(ExampleTelegramService exampleTelegramService) {
            this.exampleTelegramService = exampleTelegramService;
        }

        @Override
        public Map<String, CommandHandler> getBotCommands() {
            Map<String, CommandHandler> commands = new HashMap<>();

            commands.put("/start", (message, data) -> exampleTelegramService.start());
            commands.put("/menu", (message, data) -> exampleTelegramService.menu());

            return commands;
        }

        @Override
        public Map<String, CallbackQueryHandler> getCallbackQueries() {
            Map<String, CallbackQueryHandler> callbackQueries = new HashMap<>();

            callbackQueries.put("example_query", (callbackQuery, data) ->
                    exampleTelegramService.answer(data));

            return callbackQueries;
        }

    }

**Inyección por constructor, sin setters**: la versión anterior necesitaba
`spring.main.allow-circular-references=true` porque el `ApplicationService` pedía el `BotService` y el
`BotService` pedía el `ApplicationService`. El ciclo se rompió sacando las respuestas pendientes a
`PendingReplyRegistry`, así que esa propiedad ya no hace falta (y para un ciclo por constructor
tampoco servía).

Los handlers no reciben al usuario por parámetro: se lee de la sesión con `TelegramSecurityUtils`
(`getUser()`, `getTelegramId()`, `getChatId()`, `getRoom()`, `isPrivate()`, `isAdmin()`). De montarla
y desmontarla se encarga `AuthUpdateInterceptor`, que se recoge solo por ser un `UpdateInterceptor`
del contexto.

Si el bot juega en grupos, hay que implementar además `TelegramRoomResolver` para traducir el chat a
la sala del motor; `TelegramSecurityUtils.getRoom()` lo usa de forma perezosa.

## 3. Configuración

    spring.profiles.active=@spring.profiles.active@

    # Modo de los bots del despliegue: longpolling | webhook
    telegram.bots.type=longpolling

    # Los dos starters de telegrambots registran un bean 'telegramBotsApplication' y chocan entre sí
    # (Boot 4 no permite override de beans). Los sustituye TelegramBotsRegistrarConfig.
    spring.autoconfigure.exclude=\
      org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration,\
      org.telegram.telegrambots.webhook.starter.TelegramBotStarterConfiguration

    # Ids de Telegram con permisos de administración, separados por comas
    telegram.bots.admin-ids=${TELEGRAM_ADMIN_IDS:}

    example.bot.enabled=${EXAMPLE_BOT_ENABLED}
    example.bot.token=${EXAMPLE_BOT_TOKEN}
    example.bot.name=${EXAMPLE_BOT_NAME}

Y en modo webhook, además:

    server.port=${SERVER_PORT}

    telegram.bots.type=webhook

    example.bot.webhook.url=${EXAMPLE_BOT_WEBHOOK_URL}
    example.bot.webhook.cert.path=${EXAMPLE_BOT_WEBHOOK_CERT_PATH:}

    server.ssl.enabled=true
    server.ssl.key-store=${SERVER_KEYSTORE_FILE}
    server.ssl.key-store-password=${SERVER_KEYSTORE_PASSWORD}
    server.ssl.key-alias=${SERVER_KEYSTORE_ALIAS}
    server.ssl.key-store-type=PKCS12

    lets-encrypt-helper.domain=${LETS_ENCRYPT_DOMAIN}
    lets-encrypt-helper.contact=mailto:${LETS_ENCRYPT_EMAIL}

El endpoint que recibe los updates es **`POST /callback/{example.bot.name}`**, publicado por
`TelegramWebhookController` (el starter de webhook no publica ninguno). La URL que se declara en
`example.bot.webhook.url` tiene que apuntar ahí, y la seguridad del consumidor debe dejarlo pasar sin
autenticar.

## 4. Documentación

La arquitectura, el flujo de un update y las trampas conocidas están en
[docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md).
