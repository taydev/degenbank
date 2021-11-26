package dev.ults.degenbank.command.nft;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.NFT;
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
    public String[] getAliases() {
        return new String[]{"nfti", "info", "i", "getnft", "nft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length == 0) {
            channel.sendMessage(user.getAsMention() + ", you need to provide a name to search for (e.g. `madi_wassim`).").queue();
            return;
        }
        NFT nft = DegenBank.INSTANCE.getNFTByName(args[0]);
        if (nft != null) {
            String nftOwnerID = DegenBank.INSTANCE.getNFTOwnerID(nft);
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Color.PINK)
                    .setTitle("NFT - `" + nft.getName() + "`")
                    .addField("Creator", DegenBank.INSTANCE.getClient().getUserById(nft.getCreatorID()).getAsMention(), false)
                    .addField("Value", DegenBank.INSTANCE.getBalanceFormat().format(nft.getPrice()) + " DGN", false)
                    .addField("Owner", "<@" + nftOwnerID + ">" + (user.getId().equals(nftOwnerID) ? " (you!)" : ""), false)
                    .setImage(nft.getUrl())
                    .build()).queue();
        } else {
            channel.sendMessage(user.getAsMention() + ", this NFT does not exist!").queue();
        }
    }
}
