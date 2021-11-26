package dev.ults.degenbank.command.nft;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.utils.DegenUtils;
import dev.ults.degenbank.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;

public class NFTInfoCommand implements ICommand {
    @Override
    public String getCommand() {
        return "nftinfo";
    }

    @Override
    public String getUsage() {
        return "-nftinfo (nft name)";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"nfti", "info", "i", "getnft", "nft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length != 1) {
            channel.sendMessage(EmbedUtils.getInvalidSyntaxEmbed(this).build()).queue();
            return;
        }
        NFT nft = DegenBank.INSTANCE.getNFTById(args[0]);
        if (nft != null) {
            sendMessage(channel, EmbedUtils.getNFTInfoEmbed(nft, this.getNFTOwnerId(nft), user));
        } else {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid NFT", "The NFT you attempted to search for does not exist."));
        }
    }
}
