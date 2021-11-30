package dev.ults.degenbank.command.nft;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.utils.DegenUtils;
import dev.ults.degenbank.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class CreateNFTCommand implements ICommand {
    @Override
    public String getCommand() {
        return "createnft";
    }

    @Override
    public String getUsage() {
        return "-createnft (name) (value)";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"cnft", "makenft", "mnft", "newnft", "nnft", "mint", "mintnft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (!this.getInstance().isAcceptingTransactions()) {
            sendMessage(channel, EmbedUtils.getShutdownEmbed());
            return;
        }
        if (args.length != 2) {
            sendMessage(channel, EmbedUtils.getInvalidSyntaxEmbed(this));
            return;
        }
        if (message.getAttachments().size() != 1 || !message.getAttachments().get(0).isImage()) {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Attachment",
                    "You must include __one__ attached image for your NFT for creation to be successful."));
            return;
        }

        String name = args[0].toLowerCase();
        if (this.getNFTById(name) != null) {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Name Already Taken", "An NFT by this name already exists."));
            return;
        }

        String url = message.getAttachments().get(0).getProxyUrl();
        long value = 0;
        try {
            value = Math.abs(Long.parseLong(args[1]));
            // people keep trying to break the system, 25 min it is :(
            if (value <= 25) {
                sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Value", "An NFT cannot be minted below the set initial value (25)."));
                return;
            }
        } catch (NumberFormatException e) {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Value",
                    "An invalid value for the number of DGN to mint for was entered."));
            return;
        }

        Degen degen = this.getDegenById(user.getId());
        if (degen.removeDegenCoin(value)) {
            NFT nft = new NFT(name, user.getId(), url, value, false, value, value);
            // this is one of the few cases where I will allow a manual store, because it makes sense.
            DegenBank.INSTANCE.storeNFT(nft);
            degen.addToken(name);
            sendMessage(channel, EmbedUtils.getPingSuccessEmbed(user, "Minted NFT", String.format("Your `%s` NFT has been minted for %s!", name,
                    DegenUtils.getDisplayBalance(value))));
            // TODO: REWORK THIS \/
            DegenBank.INSTANCE.postNFTMint(nft);
        } else {
            channel.sendMessage(user.getAsMention() + ", you don't have enough DGN to mint this NFT.").queue();
        }
    }
}
