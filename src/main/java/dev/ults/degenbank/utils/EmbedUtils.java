package dev.ults.degenbank.utils;

import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.obj.Transaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.LocalDateTime;

public class EmbedUtils {

    private static EmbedBuilder getEmbed() {
        return new EmbedBuilder()
                .setTimestamp(LocalDateTime.now());
    }

    public static EmbedBuilder getSuccessEmbed(String title, String description) {
        return getEmbed()
                .setColor(Color.GREEN)
                .setTitle("Success - " + title)
                .setDescription(description);
    }

    public static MessageBuilder getPingSuccessEmbed(User user, String title, String description) {
        return new MessageBuilder().mention(user).setEmbed(getSuccessEmbed(title, description).build());
    }

    public static EmbedBuilder getErrorEmbed(String error, String description) {
        return getEmbed()
                .setColor(Color.RED)
                .setTitle("Error - " + error)
                .setDescription(description);
    }

    public static MessageBuilder getPingErrorEmbed(User user, String error, String description) {
        return new MessageBuilder().mention(user).setEmbed(getErrorEmbed(error, description).build());
    }

    public static EmbedBuilder getInvalidSyntaxEmbed(ICommand command) {
        return getErrorEmbed("Invalid Syntax", command.getUsageMessage())
                .setFooter("(arg) = required | <arg> = optional");
    }

    public static MessageBuilder getPingDegenBalanceEmbed(User user, StringBuilder sb) {
        return new MessageBuilder().mention(user).setEmbed(getEmbed()
                .setColor(Color.ORANGE)
                .setTitle(user.getName() + (user.getName().endsWith("s") ? "'" : "'s") + " Wallet")
                .setDescription(sb.toString().trim())
                .build());
    }

    public static EmbedBuilder getNFTInfoEmbed(NFT nft, String nftOwnerId, User user) {
        return getEmbed()
                .setColor(Color.PINK)
                .setTitle(String.format("NFT - `%s`", nft.getName()))
                .addField("Creator", String.format("<@%s> %s", nft.getCreatorID(), (user.getId().equals(nftOwnerId) ? " (you!)" : "")), true)
                .addField("Owner", String.format("<@%s> %s", nftOwnerId, (user.getId().equals(nftOwnerId) ? " (you!)" : "")), true)
                .addField("Initial Value", DegenUtils.getDisplayBalance(nft.getInitialPrice()), true)
                // should loop here to newline
                .addField("Last Sale Price", DegenUtils.getDisplayBalance(nft.getLastSalePrice()), true)
                .addField("For Sale?", (nft.isForSale() ? "Yes" : "No"), true)
                .addField("Sale Price", (nft.isForSale() ? DegenUtils.getDisplayBalance(nft.getSalePrice()) : "N/A"), true)
                .setImage(nft.getUrl());
    }

    public static EmbedBuilder getNFTMintEmbed(NFT nft) {
        return getEmbed()
                .setColor(Color.CYAN) // note to self - figure out what colours are actually safe to put in embeds
                .setTitle("New NFT minted!")
                .addField("NFT Name", String.format("`%s`", nft.getName()), true)
                .addField("NFT Owner", String.format("<@%s>", nft.getCreatorID()), true)
                .addField("Value", DegenUtils.getDisplayBalance(nft.getInitialPrice()), true)
                .setImage(nft.getUrl());
    }

    public static EmbedBuilder getNFTSaleEmbed(NFT nft, String buyerId) {
        return getEmbed()
                .setColor(Color.WHITE) // note to self - figure out what colours are actually safe to put in embeds
                .setTitle("NFT Purchased")
                .addField("NFT Name", String.format("`%s`", nft.getName()), true)
                .addField("New NFT Owner", String.format("<@%s>", buyerId), true)
                .addField("Value", DegenUtils.getDisplayBalance(nft.getSalePrice()), true);
    }

    public static EmbedBuilder getNFTTransferEmbed(String nftName, String sellerId, String buyerId) {
        return getEmbed()
                .setColor(Color.ORANGE) // note to self - figure out what colours are actually safe to put in embeds
                .setTitle("NFT Transferred")
                .addField("NFT Name", "`" + nftName + "`", true)
                .addField("Past NFT Owner", String.format("<@%s>", sellerId), true)
                .addField("New NFT Owner", String.format("<@%s>", buyerId), true);
    }

    public static MessageBuilder getInvalidNFTEmbed(User user, String nftName) {
        return getPingErrorEmbed(user, "Invalid NFT", String.format("The NFT `%s` does not exist, or is not owned by you.", nftName));
    }

    public static MessageBuilder getInvalidRecipientEmbed(User user) {
        return getPingErrorEmbed(user, "Invalid Recipient",
                "The recipient you attempted to send to could not be set as the target for this transaction.");
    }

    public static MessageBuilder getNFTSaleToggleEmbed(User user, NFT nft) {
        MessageBuilder builder = new MessageBuilder().mention(user);
        EmbedBuilder embed = getEmbed();
        if (nft.isForSale()) {
            embed.setColor(Color.LIGHT_GRAY)
                    .setDescription(String.format("You have set the NFT `%s` to be sale for %s. Run `-sellnft %s` to cancel this sell order.",
                            nft.getName(), nft.getSalePrice(), nft.getName()));
        } else {
            embed.setColor(Color.DARK_GRAY)
                    .setDescription(String.format("You have cancelled the sell order for the NFT `%s`.", nft.getName()));
        }
        return builder.setEmbed(embed.build());
    }

    public static EmbedBuilder getTransactionEmbed(Transaction transaction) {
        return getEmbed()
                .setColor(Color.YELLOW)
                .setTitle("Transaction #" + DegenUtils.getFormattedBalance(transaction.getTransactionId()))
                .addField("Payer", String.format("<@%s>", transaction.getPayerId()), true)
                .addField("Payee", String.format("<@%s>", transaction.getPayeeId()), true)
                .addField("Value", DegenUtils.getDisplayBalance(transaction.getValue()), true)
                .addField("Note", transaction.getNote(), false);
    }

    public static EmbedBuilder getShutdownEmbed() {
        return getEmbed()
                .setColor(Color.RED)
                .setTitle("Shutdown in Progress")
                .setDescription("degenBank is currently not accepting any new transactions or cache entries.");
    }
}
