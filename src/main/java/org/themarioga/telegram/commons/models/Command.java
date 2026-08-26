package org.themarioga.telegram.commons.models;

public class Command {

    private String command;
    private String commandData;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommandData() {
        return commandData;
    }

    public void setCommandData(String commandData) {
        this.commandData = commandData;
    }

    @Override
    public String toString() {
        return "Command{" + "command='" + command + '\'' + ", commandData='" + commandData + '\'' + '}';
    }

}
