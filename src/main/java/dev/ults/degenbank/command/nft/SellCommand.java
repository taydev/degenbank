package dev.ults.degenbank.command.nft;

import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class SellCommand implements ICommand {
    @Override
    public String getCommand() {
        return "sell";
    }

    @Override
    public String getUsage() {
        return "-sell (nft name) <price>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"sellnft", "togglesell", "togglesellnft"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (args.length > 3 || args.length < 1) {
            sendMessage(channel, EmbedUtils.getInvalidSyntaxEmbed(this));
            return;
        }

        Degen degen = this.getDegenById(user.getId());
        NFT nft = this.getNFTById(args[0]);
        if (nft != null) {
            if (degen.getOwnedTokens().contains(args[0])) {
                long salePrice = nft.getSalePrice();
                if (args.length == 2) {
                    try {
                        salePrice = Math.abs(Long.parseLong(args[1]));
                        if (salePrice <= 0) {
                            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Value", "An NFT cannot be sold for no value."));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Syntax",
                                String.format("The value you attempted to sell the NFT `%s` for is invalid.", nft.getName())));
                        return;
                    }
                    nft.setSalePrice(salePrice);
                    nft.setForSale(true);
                } else {
                    nft.setForSale(!nft.isForSale());
                    nft.setSalePrice(salePrice);
                }
                sendMessage(channel, EmbedUtils.getNFTSaleToggleEmbed(user, nft));
            } else {
                sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid NFT", String.format("You do not own the NFT `%s`", nft.getName())));
            }
        } else {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid NFT", String.format("The NFT `%s` does not exist.", args[0])));
        }
    }
}
