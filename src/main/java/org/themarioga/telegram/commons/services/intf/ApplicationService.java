package org.themarioga.telegram.commons.services.intf;

import org.themarioga.telegram.commons.models.CallbackQueryHandler;
import org.themarioga.telegram.commons.models.CommandHandler;

import java.util.Map;

public interface ApplicationService {

    Map<String, CommandHandler> getBotCommands();

    Map<String, CallbackQueryHandler> getCallbackQueries();

}
