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

public class CreateNFTCommand implements ICommand {
    @Override
    public String getCommand() {
        return "createnft";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"cnft", "makenft", "mnft", "newnft", "nnft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length != 2) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("Error - Invalid Syntax")
                    .setDescription("Usage: `-" + command + " (name) (price)`\n" +
                            "Your NFT's name must be one word (e.g. `madi_wassim`), and the image you would like to use must be attached to the " +
                            "command message.")
                    .build()).queue();
            return;
        }
        if (message.getAttachments().size() != 1 || !message.getAttachments().get(0).isImage()) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("Error - No Attachment (or too many)")
                    .setDescription("You must include **one** attached image for your NFT for creation to be successful.")
                    .build()).queue();
            return;
        }

        String name = args[0];
        if (DegenBank.INSTANCE.getNFTByName(name) != null) {
            channel.sendMessage(user.getAsMention() + ", an NFT by that name already exists.").queue();
            return;
        }

        String url = message.getAttachments().get(0).getProxyUrl();
        long value = 0;
        try {
            value = Math.abs(Long.parseLong(args[1]));
            if (value <= 0) {
                channel.sendMessage(user.getAsMention() + ", you can't mint an NFT with no initial value.").queue();
                return;
            }
        } catch (NumberFormatException e) {
            channel.sendMessage(user.getAsMention() + ", that isn't a valid amount of DGN.").queue();
            return;
        }

        Degen degen = DegenBank.INSTANCE.getDegenByID(user.getId());
        if (degen == null) {
            channel.sendMessage(user.getAsMention() + ", you haven't created a wallet! Type `-balance` to get started.").queue();
            return;
        }

        if (degen.removeDegenCoin(value)) {
            NFT nft = new NFT(name, user.getId(), url, value, false);
            DegenBank.INSTANCE.storeNFT(nft);
            degen.addToken(name);
            DegenBank.INSTANCE.storeDegen(degen);
            channel.sendMessage(user.getAsMention() + ", your `" + name + "` NFT has been minted for " +
                    DegenBank.INSTANCE.getBalanceFormat().format(value) + " DGN!").queue();
            DegenBank.INSTANCE.postNFTMint(nft);
        } else {
            channel.sendMessage(user.getAsMention() + ", you don't have enough DGN to mint this NFT.").queue();
        }
    }
}
