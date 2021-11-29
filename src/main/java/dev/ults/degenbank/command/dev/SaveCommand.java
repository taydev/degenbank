package dev.ults.degenbank.command.dev;

import dev.ults.degenbank.command.ICommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class SaveCommand implements ICommand {
    @Override
    public String getCommand() {
        return "save";
    }

    @Override
    public String getUsage() {
        return "-save";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        this.getInstance().save();
    }
}
