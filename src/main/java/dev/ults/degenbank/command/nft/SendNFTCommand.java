package dev.ults.degenbank.command.nft;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class SendNFTCommand implements ICommand {
    @Override
    public String getCommand() {
        return "sendnft";
    }

    @Override
    public String getUsage() {
        return "-sendnft (user @) (nft name)";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"givenft", "transfernft", "transfurnft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (!this.getInstance().isAcceptingTransactions()) {
            sendMessage(channel, EmbedUtils.getShutdownEmbed());
            return;
        }
        if (args.length != 2) {
            sendMessage(channel, EmbedUtils.getInvalidSyntaxEmbed(this));
        }
        Degen payer = this.getDegenById(user.getId());
        User payeeUser = this.getUserById(args[0]);
        if (payeeUser == null || payeeUser.getId().equals(user.getId())) {
            sendMessage(channel, EmbedUtils.getInvalidRecipientEmbed(user));
            return;
        }
        Degen payee = this.getDegenById(payeeUser.getId());
        String nftName = args[1];
        NFT nft = this.getNFTById(nftName);
        if (nft != null && payer.getOwnedTokens().contains(nftName)) {
            payer.removeToken(nft.getName());
            payee.addToken(nft.getName());
            // TODO: find new system for this \/
            DegenBank.INSTANCE.postNFTTrade(nft, payer.getId(), payee.getId());
            sendMessage(channel, EmbedUtils.getPingSuccessEmbed(user, "NFT Sent",
                    String.format("You have successfully sent the `%s` NFT to <@%s>.", nftName, payeeUser.getId())));
        } else {
            sendMessage(channel, EmbedUtils.getInvalidNFTEmbed(user, nftName));
        }
    }
}
