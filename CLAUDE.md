## Codebase Overview

Commons-Telegram is a small Spring Boot **library** (Maven artifact `org.themarioga:Commons-Telegram`,
versions inherited from parent POM `org.themarioga:parent:2.0.0`) that provides reusable scaffolding
for building Telegram bots — both long-polling and webhook transport modes — on top of the official
`telegram-telegrambots` Java SDK's Spring Boot starters, plus optional Let's Encrypt HTTP-01 challenge
support for self-managed webhook SSL. Consumers implement `ApplicationService` to register
`CommandHandler`/`CallbackQueryHandler` maps; the library parses incoming updates, dispatches to the
right handler, and exposes a wrapped outbound-messaging API (`BotMessageService`).

**Stack**: Java, Spring Boot, `org.telegram:telegrambots-*` (long-polling & webhook starters, client),
Maven (no explicit local versions — all inherited from parent POM), `letsencrypt-helper-tomcat`.

**Structure**: `model` (DTOs + consumer-facing functional interfaces) → `service/intf` (public
contracts: `ApplicationService`, `BotMessageService`, `BotService`) → `service/impl`
(`LongPollingBotServiceImpl`, `WebhookBotServiceImpl`, `BotMessageServiceImpl`) → `util`
(parsing/formatting helpers, webhook (de)registration) → `constants`/`config` (shared literals,
optional Let's Encrypt wiring).

⚠️ The README documents `BotCreationUtils.createLongPollingBot`/`createWebhookBot` factory methods
that do not currently exist in source — see the map's Gotchas section.

For detailed architecture, module guide, data-flow diagrams, and navigation guide, see
[docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md).
