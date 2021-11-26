package dev.ults.degenbank.command.dev;

import dev.ults.degenbank.DegenBank;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.utils.DegenUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class MintCommand implements ICommand {
    @Override
    public String getCommand() {
        return "mint";
    }

    @Override
    public String getUsage() {
        return "-mint (# of coins)";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        Degen degen = this.getDegenById(this.getInstance().getClient().getSelfUser().getId());
        long mintVolume = Long.parseLong(args[0]);
        degen.setDegenCoinBalance(mintVolume);
        DegenBank.INSTANCE.insertTransaction(user.getId(), DegenBank.INSTANCE.getClient().getSelfUser().getId(), mintVolume, "TEST MINTING - NOT " +
                "OFFICIAL START");
        channel.sendMessage(String.format("Action successful. %s has been minted and funnelled into the central wallet.",
                DegenUtils.getDisplayBalance(mintVolume))).queue();
    }
}
