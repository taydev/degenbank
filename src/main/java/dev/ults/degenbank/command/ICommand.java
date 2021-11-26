package dev.ults.degenbank.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public interface ICommand {

    String getCommand();
    default String[] getAliases() {
        return new String[0];
    }
    default boolean isDeveloperOnly() {
        return false;
    }
    void execute(User user, Message message, MessageChannel channel, String command, String[] args);

    // utility methods

}
