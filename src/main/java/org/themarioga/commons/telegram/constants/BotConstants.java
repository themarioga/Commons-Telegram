package org.themarioga.commons.telegram.constants;

public class BotConstants {

    private BotConstants() {
        throw new IllegalStateException("Constants class");
    }

    public static final String TELEGRAM_MESSAGE_TYPE_PRIVATE = "private";
    public static final String TELEGRAM_MESSAGE_TYPE_GROUP = "group";
    public static final String TELEGRAM_MESSAGE_TYPE_SUPERGROUP = "supergroup";

}
