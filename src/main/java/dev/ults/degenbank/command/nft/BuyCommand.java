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

public class BuyCommand implements ICommand {
    @Override
    public String getCommand() {
        return "buy";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"buynft"};
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

        Degen degen = DegenBank.INSTANCE.getDegenById(user.getId());
        if (degen == null) {
            channel.sendMessage(user.getAsMention() + ", you don't have a DGN wallet! Type `-balance` to get started.").queue();
            return;
        }

        NFT nft = DegenBank.INSTANCE.getNFTById(args[0]);
        if (nft != null) {
            if (nft.isForSale()) {
                if (degen.removeDegenCoin(nft.getPrice())) {
                    String id = DegenBank.INSTANCE.getNFTOwnerId(nft);
                    Degen payee = DegenBank.INSTANCE.getDegenById(id);
                    payee.addDegenCoin(nft.getPrice());
                    payee.removeToken(nft.getName());
                    degen.addToken(nft.getName());
                    DegenBank.INSTANCE.storeDegen(payee);
                    DegenBank.INSTANCE.storeDegen(degen);
                    nft.setForSale(!nft.isForSale());
                    DegenBank.INSTANCE.storeNFT(nft);
                    channel.sendMessage(user.getAsMention() + ", you have successfully purchased the `" + nft.getName() + "` NFT for " +
                            DegenBank.INSTANCE.getBalanceFormat().format(nft.getPrice()) + " DGN!").queue();
                    DegenBank.INSTANCE.postNFTBuy(nft, user.getId());
                } else {
                    channel.sendMessage(user.getAsMention() + ", you do not have enough DGN to purchase this NFT.").queue();
                }
            } else {
                channel.sendMessage(user.getAsMention() + ", that NFT is not for sale!").queue();
            }
        } else {
            channel.sendMessage(user.getAsMention() + ", that NFT does not exist.").queue();
        }
    }
}
