package dev.ults.degenbank.command.kromer;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.utils.DegenUtils;
import dev.ults.degenbank.utils.EmbedUtils;
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
    public String getUsage() {
        return "-send (user @) (nft name)";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"transfer", "transfur", "give"};
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        Degen payer = this.getDegenById(user.getId());
        User payeeUser = this.getUserById(args[0]);
        if (payeeUser == null || payeeUser.getId().equals(user.getId())) {
            sendMessage(channel, EmbedUtils.getInvalidRecipientEmbed(user));
            return;
        }
        Degen payee = this.getDegenById(payeeUser.getId());
        long paymentAmount = 0;
        try {
            paymentAmount = Math.abs(Long.parseLong(args[1]));
            if (paymentAmount <= 0) {
                sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Value", "A zero (or negative) amount of DGN cannot be sent."));
                return;
            }
        } catch (NumberFormatException ignored) {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Invalid Syntax", "The value you attempted to send is invalid."));
            return;
        }

        if (payer.removeDegenCoin(paymentAmount)) {
            payee.addDegenCoin(paymentAmount);
            sendMessage(channel, EmbedUtils.getPingSuccessEmbed(user, "Payment Sent", String.format("You have sent %s to <@%s>.",
                    DegenUtils.getDisplayBalance(paymentAmount), payeeUser.getId())));
            String note;
            if (args.length > 2) {
                note = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
            } else {
                note = "No note provided.";
            }
            // TODO: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA \/
            DegenBank.INSTANCE.insertTransaction(user.getId(), payeeUser.getId(), paymentAmount, note);
        } else {
            sendMessage(channel, EmbedUtils.getPingErrorEmbed(user, "Not Enough DGN",
                    String.format("You do not have enough DGN to complete this transaction!\n\n**DGN Needed:** %s\n**You have:**%s",
                            DegenUtils.getDisplayBalance(paymentAmount), DegenUtils.getDisplayBalance(payer.getDegenCoinBalance()))));
        }
    }
}
