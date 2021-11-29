package dev.ults.degenbank.command.dev;

import dev.ults.degenbank.command.ICommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class ShutdownCommand implements ICommand {
    @Override
    public String getCommand() {
        return "shutdown";
    }

    @Override
    public String getUsage() {
        return "-shutdown";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        channel.sendMessage("Cleaning up and shutting down...").queue();
        this.getInstance().setAcceptingTransactions(false);

    }
}
