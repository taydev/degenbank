package dev.ults.degenbank;

import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.TextSearchOptions;
import dev.ults.degenbank.command.CommandListener;
import dev.ults.degenbank.command.ICommand;
import dev.ults.degenbank.command.dev.EvalCommand;
import dev.ults.degenbank.command.dev.MintCommand;
import dev.ults.degenbank.command.dev.PullCommand;
import dev.ults.degenbank.command.dev.SandboxCommand;
import dev.ults.degenbank.command.kromer.BalanceCommand;
import dev.ults.degenbank.command.kromer.SendCommand;
import dev.ults.degenbank.command.nft.BuyCommand;
import dev.ults.degenbank.command.nft.CreateNFTCommand;
import dev.ults.degenbank.command.nft.NFTInfoCommand;
import dev.ults.degenbank.command.nft.SellCommand;
import dev.ults.degenbank.command.nft.SendNFTCommand;
import dev.ults.degenbank.obj.Degen;
import dev.ults.degenbank.obj.NFT;
import dev.ults.degenbank.obj.Transaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DegenBank {

    public static final DegenBank INSTANCE = new DegenBank();
    public static final Logger LOGGER = LoggerFactory.getLogger(DegenBank.class);

    private final MongoClient MONGO_CLIENT;
    private final TextSearchOptions CASE_INSENSITIVE = new TextSearchOptions().caseSensitive(false)
            .diacriticSensitive(false);
    private final CodecRegistry CODEC_REGISTRY;

    private String token;
    private String prefix;

    private JDA client;
    private Set<ICommand> commands;
    private MessageChannel transactionHistoryChannel;
    private List<Transaction> pendingTransactionLog;
    private Thread transactionProcessingThread;

    public DegenBank() {
        this.MONGO_CLIENT = new MongoClient();
        this.CODEC_REGISTRY = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true)
                        .register(ClassModel.builder(Degen.class).enableDiscriminator(true).build())
                        .register(ClassModel.builder(NFT.class).enableDiscriminator(true).build())
                        .register(ClassModel.builder(Transaction.class).enableDiscriminator(true).build())
                        .build()));
    }
    
    public static void main(String[] args) {
        INSTANCE.initialise();
    }

    private void initialise() {
        if (!this.parseConfiguration()) {
            LOGGER.info("Configuration not detected. Default configuration has been inserted. Do your thing, chief.");
            System.exit(69);
        }
        this.connectToDiscord();
    }

    private MongoClient getMongoClient() {
        return this.MONGO_CLIENT;
    }

    private MongoDatabase getDatabase() {
        return this.getMongoClient().getDatabase("degen_bank").withCodecRegistry(CODEC_REGISTRY);
    }

    // what am I doing? I'm putting the config in Mango.
    // why? because I'm too lazy to parse a file rn
    // deal w/ it, me in about 3 days
    private MongoCollection<Document> getConfiguration() {
        return this.getDatabase().getCollection("config");
    }

    private boolean parseConfiguration() {
        Document config = this.getConfiguration().find(Filters.eq("_id", "dev")).first();
        if (config == null) {
            Document defaultConfig = new Document()
                    .append("_id", "dev")
                    .append("token", "insert-token-here")
                    .append("prefix", "insert-prefix-here");
            this.getConfiguration().insertOne(defaultConfig);
            return false;
        } else {
            this.token = config.getString("token");
            this.prefix = config.getString("prefix");
            return true;
        }
    }

    private void connectToDiscord() {
        try {
            this.client = JDABuilder.create(this.token, Arrays.asList(GatewayIntent.values()))
                    .setAutoReconnect(true)
                    .addEventListeners(new CommandListener())
                    .setActivity(Activity.playing("with my orb"))
                    .setStatus(OnlineStatus.IDLE)
                    .build();
        } catch (LoginException e) {
            LOGGER.error("Discord login failed. Perhaps an invalid token?", e);
        }
    }

    public void onReady() {
        LOGGER.info("Discord connection established. Registering commands...");
        this.registerCommand(new SandboxCommand());
        this.registerCommand(new EvalCommand());
        this.registerCommand(new BalanceCommand());
        this.registerCommand(new MintCommand());
        this.registerCommand(new SendCommand());
        this.registerCommand(new PullCommand());
        this.registerCommand(new CreateNFTCommand());
        this.registerCommand(new NFTInfoCommand());
        this.registerCommand(new SellCommand());
        this.registerCommand(new BuyCommand());
        this.registerCommand(new SendNFTCommand());

        this.transactionHistoryChannel = this.getClient().getTextChannelById("912892124326944788");
        this.pendingTransactionLog = new CopyOnWriteArrayList<>();
        this.startTransactionProcessingThread();
    }

    public Set<ICommand> getCommands() {
        if (this.commands == null) {
            this.commands = new HashSet<>();
        }
        return this.commands;
    }

    public void registerCommand(ICommand command) {
        this.getCommands().add(command);
        LOGGER.info("Command registered - {}", command.getCommand());
    }

    public ICommand getCommand(String name) {
        for (ICommand command : this.getCommands()) {
            if (command.getCommand().equalsIgnoreCase(name) || Arrays.asList(command.getAliases()).contains(name.toLowerCase())) {
                return command;
            }
        }
        return null;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public JDA getClient() {
        return this.client;
    }

    public MongoCollection<Degen> getDegens() {
        return this.getDatabase().getCollection("degens", Degen.class);
    }

    public Degen getDegenByID(String id) {
        return this.getDegens()
                .find(Filters.eq("_id", id))
                .first();
    }

    public void storeDegen(Degen degen) {
        if (this.getDegenByID(degen.getId()) == null) {
            this.getDegens().insertOne(degen);
        } else {
            this.getDegens().findOneAndReplace(Filters.eq("_id", degen.getId()), degen);
        }
    }

    public MongoCollection<NFT> getNFTs() {
        return this.getDatabase().getCollection("nfts", NFT.class);
    }

    public NFT getNFTByName(String id) {
        return this.getNFTs()
                .find(Filters.eq("_id", id))
                .first();
    }

    public String getNFTOwnerID(NFT nft) {
        Degen degen = this.getDegens()
                .find(Filters.in("owned_tokens", nft.getName()))
                .first();
        if (degen != null) {
            return degen.getId();
        }
        return null;
    }

    public void storeNFT(NFT nft) {
        if (this.getNFTByName(nft.getName()) == null) {
            this.getNFTs().insertOne(nft);
        } else {
            this.getNFTs().findOneAndReplace(Filters.eq("_id", nft.getName()), nft);
        }
    }

    public MongoCollection<Transaction> getTransactions() {
        return this.getDatabase().getCollection("transactions", Transaction.class);
    }

    public void insertTransaction(String payerId, String payeeId, long balance, String note) {
        this.pendingTransactionLog.add(new Transaction(payerId, payeeId, balance, note));
    }

    private int getNextTransactionId() {
        Document config = this.getConfiguration().find(Filters.eq("_id", "dev")).first();
        if (config != null) {
            int transactionId = config.get("tx_index", Integer.class);
            config.replace("tx_index", transactionId + 1);
            this.getConfiguration().findOneAndReplace(Filters.eq("_id", "dev"), config);
            return transactionId;
        } else {
            // this should never be possible??????????
            return -271920;
        }
    }

    private void startTransactionProcessingThread() {
        this.transactionProcessingThread = new Thread(() -> {
            while (true) {
                if (this.pendingTransactionLog.size() > 0) {
                    Transaction transaction = this.pendingTransactionLog.get(0);
                    transaction.setTransactionId(this.getNextTransactionId());
                    this.getTransactions().insertOne(transaction);
                    this.transactionHistoryChannel.sendMessage(new EmbedBuilder()
                            .setColor(Color.ORANGE)
                            .setTitle("Transaction #" + this.getBalanceFormat().format(transaction.getTransactionId()))
                            .addField("Payer", this.getClient().getUserById(transaction.getPayerId()).getAsMention(), true)
                            .addField("Payee", this.getClient().getUserById(transaction.getPayeeId()).getAsMention(), true)
                            .addField("Value", this.getBalanceFormat().format(transaction.getBalance()) + " DGN", true)
                            .addField("Note", transaction.getNote(), false)
                            .setTimestamp(LocalDateTime.now())
                            .build()).queue();
                    this.pendingTransactionLog.remove(transaction);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }, "TransactionProcessingThread");
        this.transactionProcessingThread.start();
    }

    public void postNFTMint(NFT nft) {
        this.transactionHistoryChannel.sendMessage(new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("New NFT minted!")
                .addField("NFT Name", "`" + nft.getName() + "`", true)
                .addField("NFT Owner", this.getClient().getUserById(nft.getCreatorID()).getName(), true)
                .addField("Value", this.getBalanceFormat().format(nft.getPrice()) + " DGN", true)
                .setImage(nft.getUrl())
                .setTimestamp(LocalDateTime.now())
                .build()).queue();
    }

    public void postNFTBuy(NFT nft, String buyerId) {
        this.transactionHistoryChannel.sendMessage(new EmbedBuilder()
                .setColor(Color.WHITE)
                .setTitle("NFT Purchased")
                .addField("NFT Name", "`" + nft.getName() + "`", true)
                .addField("New NFT Owner", this.getClient().getUserById(buyerId).getAsMention(), true)
                .addField("Value", this.getBalanceFormat().format(nft.getPrice()) + " DGN", true)
                .setTimestamp(LocalDateTime.now())
                .build()).queue();
    }

    public void postNFTTrade(NFT nft, String payerId, String payeeId) {
        this.transactionHistoryChannel.sendMessage(new EmbedBuilder()
                .setColor(Color.MAGENTA)
                .setTitle("NFT Transferred")
                .addField("NFT Name", "`" + nft.getName() + "`", true)
                .addField("Past NFT Owner", this.getClient().getUserById(payerId).getAsMention(), true)
                .addField("New NFT Owner", this.getClient().getUserById(payeeId).getAsMention(), true)
                .setTimestamp(LocalDateTime.now())
                .build()).queue();
    }

    public DecimalFormat getBalanceFormat() {
        return new DecimalFormat("#,###,###,##0");
    }
}
