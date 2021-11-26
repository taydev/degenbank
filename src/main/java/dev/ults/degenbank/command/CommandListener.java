package dev.ults.degenbank.command;

import dev.ults.degenbank.DegenBank;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

// this entire system should probably be swapped for Discord's proprietary command system
// will I? probably not.
public class CommandListener extends ListenerAdapter {

    @Override
    // not actually sure why IntelliJ makes this a warn when the event isn't used
    // should probably look into that
    public void onReady(@NotNull ReadyEvent e) {
        DegenBank.INSTANCE.onReady();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (e.getAuthor().isBot()) return;
        String messageContent = e.getMessage().getContentRaw();
        if (!messageContent.startsWith(DegenBank.INSTANCE.getPrefix())) return;

        messageContent = messageContent.substring(DegenBank.INSTANCE.getPrefix().length());
        String[] parts = messageContent.split(" ");
        String commandString = parts[0];
        String[] args = Arrays.stream(parts).skip(1).toArray(String[]::new);
        User user = e.getAuthor();
        Message message = e.getMessage();
        MessageChannel channel = e.getChannel();

        ICommand command = DegenBank.INSTANCE.getCommand(commandString);
        if (command != null) {
            // hardcoded identity checks lmao
            if (command.isDeveloperOnly() && !user.getId().equals("277385484919898112")) return;
            DegenBank.LOGGER.info("User {}#{} ({}) dispatching command {} in #{} ({}) with parameters: {}",
                    user.getName(),
                    user.getDiscriminator(),
                    user.getId(),
                    commandString,
                    channel.getName(),
                    channel.getId(),
                    args);
            command.execute(user, message, channel, commandString, args);
        }
    }
}
