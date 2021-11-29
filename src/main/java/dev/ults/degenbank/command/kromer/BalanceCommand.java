package dev.ults.degenbank.command.kromer;

import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.utils.DegenUtils;
import dev.ults.degenbank.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class BalanceCommand implements ICommand {
    @Override
    public String getCommand() {
        return "balance";
    }

    @Override
    public String getUsage() {
        return "-balance";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"bal", "b", "kromer", "k", "degencoin", "coin"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        if (!this.getInstance().isAcceptingTransactions()) {
            sendMessage(channel, EmbedUtils.getShutdownEmbed());
            return;
        }
        Degen degen = this.getDegenById(user.getId());
        long total = degen.getDegenCoinBalance();
        StringBuilder sb = new StringBuilder()
                .append("**Degen Coin:** ").append(DegenUtils.getDisplayBalance(degen.getDegenCoinBalance()));
        if (!degen.getOwnedTokens().isEmpty()) {
            sb.append("\n\n**__NFTs:__**");
            for (String entry : degen.getOwnedTokens()) {
                NFT nft = this.getNFTById(entry);
                total += nft.getLastSalePrice();
                sb.append("\n- `").append(nft.getName()).append("`: ")
                        .append(DegenUtils.getDisplayBalance(nft.getLastSalePrice()));
            }
        }
        sb.append("\n\n**Total:** ").append(DegenUtils.getDisplayBalance(total));
        sendMessage(channel, EmbedUtils.getPingDegenBalanceEmbed(user, sb));
    }
}
