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

public class SendNFTCommand implements ICommand {
    @Override
    public String getCommand() {
        return "sendnft";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"givenft", "transfernft", "transfurnft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length != 2) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("Error - Invalid Syntax")
                    .setDescription("Usage: `-" + command + " (user) (nft name)`")
                    .build()).queue();
        }
        Degen payer = DegenBank.INSTANCE.getDegenByID(user.getId());
        if (payer == null) {
            channel.sendMessage(user.getAsMention() + ", you have no DGN wallet! Type `-balance` to start.").queue();
            return;
        }
        User payeeUser = DegenBank.INSTANCE.getClient().getUserById(args[0].replaceAll("[^0-9]", ""));
        if (payeeUser == null) {
            channel.sendMessage(user.getAsMention() + ", that user doesn't exist.").queue();
            return;
        } else if (payeeUser.getId().equals(user.getId())) {
            channel.sendMessage(user.getAsMention() + ", you can't send an NFT to yourself.").queue();
            return;
        }
        Degen payee = DegenBank.INSTANCE.getDegenByID(payeeUser.getId());
        if (payee == null) {
            channel.sendMessage(user.getAsMention() + ", the recipient doesn't have a DGN wallet. Tell them to create one with `-balance`.").queue();
            return;
        }

        String nftName = args[1];
        if (payer.getOwnedTokens().contains(nftName)) {
            NFT nft = DegenBank.INSTANCE.getNFTByName(nftName);
            if (nft != null) {
                payer.removeToken(nft.getName());
                payee.addToken(nft.getName());
                DegenBank.INSTANCE.storeDegen(payer);
                DegenBank.INSTANCE.storeDegen(payee);
                DegenBank.INSTANCE.postNFTTrade(nft, payer.getId(), payee.getId());
                channel.sendMessage(user.getAsMention() + ", you have successfully sent the `" + nft.getName() + "`!").queue();
            } else {
                channel.sendMessage(user.getAsMention() + ", that NFT does not exist.").queue();
            }
        } else {
            channel.sendMessage(user.getAsMention() + ", you do not own that NFT.").queue();
        }
    }
}
