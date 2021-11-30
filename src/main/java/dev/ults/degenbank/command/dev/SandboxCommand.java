package dev.ults.degenbank.command.dev;

import com.mongodb.client.MongoCursor;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.obj.Degen;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;

public class SandboxCommand implements ICommand {
    @Override
    public String getCommand() {
        return "sandbox";
    }

    @Override
    public String getUsage() {
        return "-sandbox <(<><()<><)><(><)><(><)><(><)(>)>)>";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }

    @Override
    public void execute(User user, Message message, MessageChannel channel, String command, String[] args) {
        MongoCursor<Degen> cursor = this.getInstance().getDegens().find().cursor();
        while (cursor.hasNext()) {
            Degen degen = cursor.next();
            for (String s : degen.getOwnedTokens()) {
                degen.removeToken(s);
                degen.addToken(s.toLowerCase());
            }
            this.getInstance().storeDegen(degen);
        }
    }
}
