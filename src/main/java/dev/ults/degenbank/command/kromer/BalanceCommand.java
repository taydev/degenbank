package dev.ults.degenbank.command.kromer;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.Map;

public class BalanceCommand implements ICommand {
    @Override
    public String getCommand() {
        return "balance";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"bal", "b", "kromer", "k", "degencoin", "coin"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        Degen degen = DegenBank.INSTANCE.getDegenByID(user.getId());
        if (degen == null) {
            channel.sendMessage("No wallet found. Creating...").queue();
            degen = new Degen(user.getId());
            DegenBank.INSTANCE.storeDegen(degen);
        }
        long total = degen.getDegenCoinBalance();
        StringBuilder sb = new StringBuilder()
                .append("**Degen Coin:** ").append(DegenBank.INSTANCE.getBalanceFormat().format(degen.getDegenCoinBalance()))
                .append(" DGN");
        if (!degen.getOwnedTokens().isEmpty()) {
            sb.append("\n\n**__NFTs:__**");
            for (String entry : degen.getOwnedTokens()) {
                NFT nft = DegenBank.INSTANCE.getNFTByName(entry);
                total += nft.getPrice();
                sb.append("\n- `").append(nft.getName()).append("`: ")
                        .append(DegenBank.INSTANCE.getBalanceFormat().format(nft.getPrice()))
                        .append(" DGN");
            }
        }
        sb.append("\n\n**Total:** ").append(DegenBank.INSTANCE.getBalanceFormat().format(total)).append(" DGN");
        MessageBuilder mb = new MessageBuilder()
                .mention(user)
                .setEmbed(new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setTitle(user.getName() + (user.getName().endsWith("s") ? "'" : "'s") + " Balance")
                        .setDescription(sb.toString().trim())
                        .build());
        channel.sendMessage(mb.build()).queue();
    }
}
