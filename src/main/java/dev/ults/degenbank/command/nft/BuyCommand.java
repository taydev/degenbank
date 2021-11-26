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

public class BuyCommand implements ICommand {
    @Override
    public String getCommand() {
        return "buy";
    }

    @Override
    public String getUsage() {
        return "-buy (nft name)";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"buynft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length != 1) {
            sendMessage(channel, EmbedUtils.getInvalidSyntaxEmbed(this));
        }

        Degen degen = this.getDegenById(user.getId());
        NFT nft = this.getNFTById(args[0]);
        if (nft != null) {
            if (nft.isForSale()) {
                if (degen.removeDegenCoin(nft.getSalePrice())) {
                    String id = this.getNFTOwnerId(nft);
                    Degen payee = this.getDegenById(id);
                    payee.addDegenCoin(nft.getSalePrice());
                    payee.removeToken(nft.getName());
                    degen.addToken(nft.getName());
                    nft.setLastSalePrice(nft.getSalePrice());
                    nft.setForSale(!nft.isForSale());
                    sendMessage(channel, EmbedUtils.getPingSuccessEmbed(user, "NFT Purchased",
                            String.format("You have successfully purchased the `%s` NFT for %s!", nft.getName(),
                                    DegenUtils.getDisplayBalance(nft.getLastSalePrice()))));
                    // TODO: AAAAAAAAAAA \/
                    DegenBank.INSTANCE.postNFTBuy(nft, user.getId());
                } else {
                    sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Not Enough DGN",
                            String.format("You do not have enough DGN to complete this purchase!\n\n**DGN Needed:** %s\n**You have:**%s",
                                    DegenUtils.getDisplayBalance(nft.getSalePrice()), DegenUtils.getDisplayBalance(degen.getDegenCoinBalance()))));
                }
            } else {
                sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Unavailable to Buy", "The NFT you attempted to purchase is not for sale."));
            }
        } else {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid NFT", "The NFT you attempted to buy does not exist."));
        }
    }
}
