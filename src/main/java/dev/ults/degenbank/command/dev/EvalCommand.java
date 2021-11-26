package dev.ults.degenbank.command.dev;

import dev.ults.degenbank.command.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// base source is from an old version of https://github.com/flarebot/flarebot. thanks arsen :)
public class EvalCommand implements ICommand {
    private ScriptEngineManager manager = new ScriptEngineManager();
    private static final ThreadGroup EVALS = new ThreadGroup("EvalCommand Thread Pool");
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> new Thread(EVALS, r,
            EVALS.getName() + EVALS.activeCount()));
    private static final List<String> IMPORTS = Arrays.asList("dev.ults.degenbank",
            "dev.ults.degenbank.obj",
            "dev.ults.degenbank.command",
            "dev.ults.degenbank.command.dev",
            "dev.ults.degenbank.command.kromer",
            "net.dv8tion.jda.core",
            "net.dv8tion.jda.core.managers",
            "net.dv8tion.jda.core.entities.impl",
            "net.dv8tion.jda.core.entities",
            "java.util.streams",
            "java.util",
            "java.text",
            "java.lang",
            "java.math",
            "java.time",
            "java.io",
            "java.nio",
            "java.nio.files",
            "java.util.stream");

    @Override
    public void execute(User sender, Message message, MessageChannel channel, String commandString, String[] args) {
        String imports = IMPORTS.stream().map(s -> "Packages." + s).collect(Collectors.joining(", ", "var imports = new JavaImporter(", ");\n"));
        ScriptEngine engine = manager.getEngineByName("nashorn");
        engine.put("channel", channel);
        engine.put("guild", message.getGuild());
        engine.put("message", message);
        engine.put("jda", sender.getJDA());
        engine.put("sender", sender);
        String code = String.join(" ", args);
        POOL.submit(() -> {
            try {
                String eResult = String.valueOf(engine.eval(imports + "with (imports) {\n" + code + "\n}"));
                if (("```js\n" + eResult + "\n```").length() > 1048) {
                    eResult = eResult.substring(0, 1984);
                }
                eResult = "```js\n" + eResult + "\n```";
                channel.sendMessage(new EmbedBuilder()
                                .setAuthor(sender.getName(), sender.getAvatarUrl())
                                .addField("Code:", "```js\n" + code + "```", false)
                                .addField("Result: ", eResult, false).build()).queue();
            } catch (Exception e) {
                channel.sendMessage(e.getMessage()).queue();
            }
        });
    }

    @Override
    public String getCommand() {
        return "eval";
    }

    @Override
    public boolean isDeveloperOnly() {
        return true;
    }
}
