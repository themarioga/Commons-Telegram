package org.themarioga.commons.telegram.services.intf;

import org.themarioga.commons.telegram.models.CallbackQueryHandler;
import org.themarioga.commons.telegram.models.CommandHandler;

import java.util.Map;

public interface ApplicationService {

    Map<String, CommandHandler> getBotCommands();

    Map<String, CallbackQueryHandler> getCallbackQueries();

}
