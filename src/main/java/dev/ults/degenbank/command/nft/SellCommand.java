package dev.ults.degenbank.command.nft;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;

public class SellCommand implements ICommand {
    @Override
    public String getCommand() {
        return "sell";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"sellnft", "togglesell", "togglesellnft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length != 1) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("Error - Invalid Syntax")
                    .setDescription("Usage: `-" + command + " (nft name)`")
                    .build()).queue();
            return;
        }

        Degen degen = DegenBank.INSTANCE.getDegenByID(user.getId());
        if (degen == null) {
            channel.sendMessage(user.getAsMention() + ", you don't have a DGN wallet! Type `-balance` to get started.").queue();
            return;
        }

        if (degen.getOwnedTokens().contains(args[0])) {
            NFT nft = DegenBank.INSTANCE.getNFTByName(args[0]);
            if (nft != null) {
                nft.setForSale(!nft.isForSale());
                if (nft.isForSale()) {
                    channel.sendMessage(user.getAsMention() + ", you have marked the NFT `" + nft.getName() + "` as being available for sale. " +
                            "Run this command again to remove this mark.").queue();
                } else {
                    channel.sendMessage(user.getAsMention() + ", you have removed the NFT `" + nft.getName() + "`'s mark for sale.").queue();
                }
                System.out.println(nft.isForSale());
                DegenBank.INSTANCE.storeNFT(nft);
            } else {
                channel.sendMessage(user.getAsMention() + ", by some feat of RNG, you have broken the bot. Congratulations.").queue();
            }
        } else {
            channel.sendMessage(user.getAsMention() + ", you do not own that NFT.").queue();
        }
    }
}
