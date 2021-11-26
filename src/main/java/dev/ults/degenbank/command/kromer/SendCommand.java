package dev.ults.degenbank.command.kromer;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SendCommand implements ICommand {
    @Override
    public String getCommand() {
        return "send";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"transfer", "transfur", "give"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        Degen payer = DegenBank.INSTANCE.getDegenByID(user.getId());
        if (payer == null) {
            channel.sendMessage(user.getAsMention() + ", you don't have a DGN wallet. Type `-balance` to get started.").queue();
            return;
        }
        User payeeUser = DegenBank.INSTANCE.getClient().getUserById(args[0].replaceAll("[^0-9]", ""));
        if (payeeUser == null) {
            channel.sendMessage(user.getAsMention() + ", that user doesn't exist.").queue();
            return;
        } else if (payeeUser.getId().equals(user.getId())) {
            channel.sendMessage(user.getAsMention() + ", you can't send money to yourself.").queue();
            return;
        }
        Degen payee = DegenBank.INSTANCE.getDegenByID(payeeUser.getId());
        if (payee == null) {
            channel.sendMessage(user.getAsMention() + ", the recipient doesn't have a DGN wallet. Tell them to create one with `-balance`.").queue();
            return;
        }

        long paymentAmount = 0;
        try {
            paymentAmount = Math.abs(Long.parseLong(args[1]));
        } catch (NumberFormatException ignored) {
            channel.sendMessage(user.getAsMention() + ", that isn't a valid amount of DGN.").queue();
            return;
        }

        if (payer.removeDegenCoin(paymentAmount)) {
            payee.addDegenCoin(paymentAmount);
            channel.sendMessage(String.format("Success! %s, you have sent %s DGN to %s.",
                    user.getAsMention(),
                    DegenBank.INSTANCE.getBalanceFormat().format(paymentAmount),
                    payeeUser.getAsMention())).queue();
            String note;
            if (args.length > 2) {
                note = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
            } else {
                note = "No note provided.";
            }
            DegenBank.INSTANCE.insertTransaction(user.getId(), payeeUser.getId(), paymentAmount, note);
            DegenBank.INSTANCE.storeDegen(payer);
            DegenBank.INSTANCE.storeDegen(payee);
        } else {
            channel.sendMessage(user.getAsMention() + ", you don't have enough DGN to do this.").queue();
        }
    }
}
