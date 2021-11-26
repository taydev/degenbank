package dev.ults.degenbank.command.dev;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PullCommand implements ICommand {
    @Override
    public String getCommand() {
        return "pull";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        String botId = DegenBank.INSTANCE.getClient().getSelfUser().getId();
        Degen payer = DegenBank.INSTANCE.getDegenById(botId);
        Degen payee = DegenBank.INSTANCE.getDegenById(user.getId());
        long paymentAmount = Long.parseLong(args[0]);
        if (payer.removeDegenCoin(paymentAmount)) {
            payee.addDegenCoin(paymentAmount);
            channel.sendMessage(String.format("Success! %s, you have withdrawed %s DGN from the reserves.",
                    user.getAsMention(),
                    DegenBank.INSTANCE.getBalanceFormat().format(paymentAmount)
            )).queue();
            DegenBank.INSTANCE.insertTransaction(botId, user.getId(), paymentAmount, "Reserves transfer.");
            DegenBank.INSTANCE.storeDegen(payer);
            DegenBank.INSTANCE.storeDegen(payee);
        }
    }
}
