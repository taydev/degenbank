package dev.ults.degenbank.command.dev;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.utils.DegenUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class PullCommand implements ICommand {

    @Override
    public String getCommand() {
        return "pull";
    }

    @Override
    public String getUsage() {
        return "-pull (value)";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        String botId = this.getInstance().getClient().getSelfUser().getId();
        Degen payer = this.getDegenById(botId);
        Degen payee = this.getDegenById(user.getId());
        long paymentAmount = Long.parseLong(args[0]);
        if (payer.removeDegenCoin(paymentAmount)) {
            payee.addDegenCoin(paymentAmount);
            channel.sendMessage(String.format("Success! %s, you have withdrawn %s from the reserves.",
                    user.getAsMention(),
                    DegenUtils.getDisplayBalance(paymentAmount)
            )).queue();
            DegenBank.INSTANCE.insertTransaction(botId, user.getId(), paymentAmount, "Reserves transfer.");
        }
    }
}
