package dev.ults.degenbank.command;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public interface ICommand {

    String getCommand();
    String getUsage();
    default String[] getAliases() {
        return new String[0];
    }
    default boolean isDeveloperOnly() {
        return false;
    }
    void execute(User user, Message message, MessageChannel channel, String command, String[] args);

    // utility methods - shouldn't really need to be overridden ever
    default String getUsageMessage() {
        return String.format("Usage: `%s`", this.getUsage());
    }
    default DegenBank getInstance() {
        return DegenBank.INSTANCE;
    }
    default Degen getDegenById(String id) {
        return this.getInstance().getDegenById(id.replaceAll("[^0-9]", ""));
    }
    default User getUserById(String id) {
        return this.getInstance().getClient().getUserById(id.replaceAll("[^0-9]", ""));
    }
    default NFT getNFTById(String id) {
        return this.getInstance().getNFTById(id);
    }
    default String getNFTOwnerId(NFT nft) {
        return this.getInstance().getNFTOwnerId(nft);
    }
    default void sendMessage(MessageChannel channel, MessageBuilder builder) {
        channel.sendMessage(builder.build()).queue();
    }
    default void sendMessage(MessageChannel channel, EmbedBuilder builder) {
        channel.sendMessage(builder.build()).queue();
    }

}
