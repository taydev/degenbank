package dev.ults.degenbank.command.kromer;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.utils.DegenUtils;
import dev.ults.degenbank.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceTopCommand implements ICommand{
    @Override
    public String getCommand() {
        return "balancetop";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"baltop", "btop", "top"};
    }

    @Override
    public String getUsage() {
        return "-balancetop";
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        this.getInstance().save();
        MongoCursor<Degen> cursor = this.getInstance().getDegens().find().cursor();
        List<Degen> degensWithBalance = new ArrayList<>();
        while (cursor.hasNext()) {
            Degen degen = cursor.next();
            if (degen.getId().equals("912870069678260267")) continue;
            if (degen.getTotalBalance() >= 0) {
                degensWithBalance.add(degen);
            }
        }
        degensWithBalance.sort(Comparator.comparing(Degen::getTotalBalance).reversed());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            Degen target = degensWithBalance.get(i);
            User targetUser = this.getInstance().getClient().getUserById(target.getId());
            if (targetUser == null) {
                sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Internal User", "Tell ults the baltop command is broken."));
                return;
            }
            sb.append("**").append(i+1).append("**: ");
            if (i < 3) {
                sb.append("**");
            }
            sb.append(targetUser.getName()).append("#").append(targetUser.getDiscriminator())
                    .append(" (").append(DegenUtils.getDisplayBalance(target.getTotalBalance())).append(")");
            if (i < 3) {
                sb.append("**");
            }
            sb.append("\n");
        }
        sendMessage(channel, EmbedUtils.getDegenLeaderboardEmbed(sb));
    }
}
