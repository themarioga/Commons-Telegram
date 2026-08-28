## Codebase Overview

Commons-Telegram es la **librería** (Maven `org.themarioga:commons-telegram`) con lo que comparten
todos los bots de Telegram del proyecto: la identidad de quien escribe, la sesión de cada update, el
reparto a los handlers y el envío de mensajes. No sabe nada de ningún juego concreto; la usan
`CAH-Telegram` y, cuando exista, el bot de SH.

Se apoya en `commons-engine` para el modelo de usuario y el contexto de seguridad, de modo que un
handler puede preguntar quién habla sin que el motor sepa qué es Telegram.

**Stack**: Java, Spring Boot 4.1, Hibernate/JPA (solo la tabla `telegram_user`), Spring Security
(contexto programático), `org.telegram:telegrambots-*`, `letsencrypt-helper-tomcat`, JUnit 5 +
Mockito.

**Structure**: `models` (DTOs de comando/callback, los interfaces funcionales que implementa el
consumidor, y la entidad `TelegramUser`) → `dao` → `services/intf` (`ApplicationService`,
`BotMessageService`, `BotService`, `TelegramUserService`, `TelegramRoomResolver`) → `services/impl`
(los dos transportes, el `UpdateDispatcher` común, el interceptor de sesión y los registros en
memoria) → `security` (sesión y acceso a ella) → `config` (arranque de los bots, webhook, Let's
Encrypt, administradores) → `util`.

⚠️ Cosas que conviene saber antes de tocar nada:

- **Esta librería sustituye a las autoconfiguraciones de los dos starters de telegrambots**, que
  declaran el mismo bean y no pueden convivir; hay que excluirlas y el consumidor arranca por
  `TelegramBotsRegistrarConfig`.
- **El starter de webhook no publica ningún endpoint**: lo pone `TelegramWebhookController`.
- **La sesión se monta y se desmonta por update.** Si algo se ejecuta en otro hilo, tiene que ir
  envuelto en `TelegramSession`, o el motor no encontrará al usuario.
- **El webhook ya recibe updates reales de Telegram**, pero nada aguas abajo del controller se ha
  probado contra un bot real.

Para la arquitectura, el flujo de un update y el resto de trampas, ver
[docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md). Para el ejemplo de uso, [README.md](README.md).
