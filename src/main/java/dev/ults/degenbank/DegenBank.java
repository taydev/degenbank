package dev.ults.degenbank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
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
import dev.ults.degenbank.utils.DegenUtils;
import dev.ults.degenbank.utils.EmbedUtils;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DegenBank {

    //region Global Variables
    public static final DegenBank INSTANCE = new DegenBank();
    public static final Logger LOGGER = LoggerFactory.getLogger(DegenBank.class);
    private final String ENV;
    //endregion

    //region MongoDB Variables
    private final MongoClient MONGO_CLIENT;
    private final FindOneAndReplaceOptions UPSERT_OPTS;
    private final CodecRegistry CODEC_REGISTRY;
    //endregion

    //region Bot/JDA Variables
    private String token;
    private String prefix;
    private JDA client;
    private Set<ICommand> commands;
    //endregion

    //region Caching and Transaction Variables
    private Set<Degen> cachedDegens;
    private Set<NFT> cachedNFTs;
    private boolean acceptingTransactions;
    private MessageChannel transactionHistoryChannel;
    private List<Transaction> pendingTransactionLog;
    private final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(4);
    private Thread transactionProcessingThread;
    //endregion

    public DegenBank() {
        this.ENV = Optional.ofNullable(System.getenv("ENV")).orElse("DEV");
        this.MONGO_CLIENT = new MongoClient();
        this.UPSERT_OPTS = new FindOneAndReplaceOptions().upsert(true);
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

    //region Initialisation Methods
    private void initialise() {
        if (!this.parseConfiguration()) {
            System.exit(69);
        }
        this.connectToDiscord();
    }

    private boolean parseConfiguration() {
        File file = new File(this.getEnvironment().toLowerCase() + "_config.json");
        try {
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                JsonObject configObject = (JsonObject) JsonParser.parseReader(reader);
                this.setToken(configObject.get("token").getAsString());
                this.setPrefix(configObject.get("prefix").getAsString());
                reader.close();
                LOGGER.info("Configuration parsed. Moving to Discord loading phase...");
                return true;
            } else {
                FileWriter writer = new FileWriter(file);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.addProperty("token", "insert-token-here");
                defaultConfig.addProperty("prefix", "insert-prefix-here");
                writer.write(gson.toJson(defaultConfig));
                writer.flush();
                writer.close();
                LOGGER.info("Default configuration created. Insert your token and prefix of choice, then re-launch the bot.");
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while parsing configuration.", e);
        }
        return false;
    }

    private void connectToDiscord() {
        try {
            this.client = JDABuilder.create(this.token, Arrays.asList(GatewayIntent.values()))
                    .setAutoReconnect(true)
                    .addEventListeners(new CommandListener())
                    // really annoys me that the bot API doesn't support custom statuses. discord, fix pls.
                    // unless it's jda, then uhh... jda, fix pls.
                    .setActivity(Activity.playing("with my orb"))
                    .setStatus(OnlineStatus.IDLE)
                    .build();
        } catch (LoginException e) {
            LOGGER.error("Discord login failed. Perhaps an invalid token?", e);
        }
    }

    public void onReady() {
        LOGGER.info("Discord connection established. Registering commands...");

        // -- Developer Commands --
        this.registerCommand(new EvalCommand());
        this.registerCommand(new MintCommand());
        this.registerCommand(new PullCommand());
        this.registerCommand(new SandboxCommand());
        // -- Kromer Commands
        this.registerCommand(new BalanceCommand());
        this.registerCommand(new SendCommand());
        // -- NFT Commands --
        this.registerCommand(new BuyCommand());
        this.registerCommand(new CreateNFTCommand());
        this.registerCommand(new NFTInfoCommand());
        this.registerCommand(new SellCommand());
        this.registerCommand(new SendNFTCommand());

        LOGGER.info("Commands registered. Initialising transaction processing...");
        this.initialiseTransactionHistoryChannel();
        this.initialisePendingTransactionLog();
        this.startTransactionProcessingThread();

        this.cachedDegens = new HashSet<>();
        this.cachedNFTs = new HashSet<>();

        this.setAcceptingTransactions(true);
    }

    private void startTransactionProcessingThread() {
        threadPool.scheduleAtFixedRate(new Thread(() -> {
            if (this.getPendingTransactionLog().size() > 0) {
                Transaction transaction = this.getPendingTransactionLog().get(0);
                transaction.setTransactionId(this.getNextTransactionId());
                this.getTransactions().insertOne(transaction);
                this.getTransactionHistoryChannel().sendMessage(EmbedUtils.getTransactionEmbed(transaction).build()).queue();
                this.getPendingTransactionLog().remove(transaction);
            }
            if (!this.isAcceptingTransactions() && this.getPendingTransactionLog().size() == 0) {
                threadPool.shutdown();
            }
        }), 5, 5, TimeUnit.SECONDS);
    }
    //endregion

    //region Global - Getters
    public String getEnvironment() {
        return this.ENV;
    }
    //endregion

    //region MongoDB - Getters
    private MongoClient getMongoClient() {
        return this.MONGO_CLIENT;
    }

    private MongoDatabase getDatabase() {
        return this.getMongoClient().getDatabase("degen_bank").withCodecRegistry(this.CODEC_REGISTRY);
    }

    public MongoCollection<Degen> getDegens() {
        return this.getDatabase().getCollection("degens", Degen.class);
    }

    public MongoCollection<NFT> getNFTs() {
        return this.getDatabase().getCollection("nfts", NFT.class);
    }

    public MongoCollection<Transaction> getTransactions() {
        return this.getDatabase().getCollection("transactions", Transaction.class);
    }
    //endregion

    //region Bot/JDA - Getters
    private String getToken() {
        return this.token;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public Set<ICommand> getCommands() {
        if (this.commands == null) {
            this.commands = new HashSet<>();
        }
        return this.commands;
    }

    public ICommand getCommand(String name) {
        for (ICommand command : this.getCommands()) {
            if (command.getCommand().equalsIgnoreCase(name) || Arrays.asList(command.getAliases()).contains(name.toLowerCase())) {
                return command;
            }
        }
        return null;
    }

    public JDA getClient() {
        return this.client;
    }
    //endregion

    //region Caching and Transactions - Getters
    public MessageChannel getTransactionHistoryChannel() {
        return this.transactionHistoryChannel;
    }

    public List<Transaction> getPendingTransactionLog() {
        return this.pendingTransactionLog;
    }

    public boolean isAcceptingTransactions() {
        return this.acceptingTransactions;
    }

    public Set<Degen> getCachedDegens() {
        return this.cachedDegens;
    }

    public Degen getDegenById(String id) {
        Optional<Degen> opt = this.getCachedDegens().stream().filter(degen -> degen.getId().equals(id)).findFirst();
        if (opt.isPresent()) {
            return opt.get();
        } else {
            Degen degen = this.getDegens()
                    .find(Filters.eq("_id", id))
                    .first();
            if (degen == null) {
                degen = new Degen(id);
            }
            this.getCachedDegens().add(degen);
            return degen;
        }
    }

    public Set<NFT> getCachedNFTs() {
        return this.cachedNFTs;
    }

    public NFT getNFTById(String id) {
        Optional<NFT> opt = this.getCachedNFTs().stream().filter(nft -> nft.getName().equals(id)).findFirst();
        if (opt.isPresent()) {
            return opt.get();
        } else {
            NFT nft = this.getNFTs()
                    .find(Filters.eq("_id", id))
                    .first();
            if (nft != null) {
                this.getCachedNFTs().add(nft);
                return nft;
            }
            return null;
        }
    }

    public String getNFTOwnerId(NFT nft) {
        Degen degen = this.getDegens()
                .find(Filters.in("owned_tokens", nft.getName()))
                .first();
        if (degen != null) {
            return degen.getId();
        }
        return null;
    }

    private int getNextTransactionId() {
        return (int) (this.getTransactions().countDocuments() + 1);
    }
    //endregion

    //region MongoDB - Storage (Setters, technically)
    public void storeDegen(Degen degen) {
        this.getDegens().findOneAndReplace(Filters.eq("_id", degen.getId()), degen, this.UPSERT_OPTS);
    }

    public void storeNFT(NFT nft) {
        this.getNFTs().findOneAndReplace(Filters.eq("_id", nft.getName()), nft, this.UPSERT_OPTS);
    }
    //endregion

    //region Bot/JDA - Setters
    private void setToken(String token) {
        this.token = token;
    }

    private void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    // this is technically a setter, I do not care
    public void registerCommand(ICommand command) {
        this.getCommands().add(command);
        LOGGER.info("Command registered - {}", command.getCommand());
    }
    //endregion

    //region Caching and Transactions - Setters
    public void initialiseTransactionHistoryChannel() {
        this.transactionHistoryChannel = this.getClient().getTextChannelById("912892124326944788");
    }

    public void initialisePendingTransactionLog() {
        this.pendingTransactionLog = new CopyOnWriteArrayList<>();
    }

    public void setAcceptingTransactions(boolean isAcceptingTransactions) {
        this.acceptingTransactions = isAcceptingTransactions;
    }

    public void insertTransaction(String payerId, String payeeId, long balance, String note) {
        this.getPendingTransactionLog().add(new Transaction(payerId, payeeId, balance, note));
    }
    //endregion

    // do these really need their own region? no. am I going to give them one? yes.
    //region Transaction History Posting Methods
    public void postNFTMint(NFT nft) {
        this.transactionHistoryChannel.sendMessage(EmbedUtils.getNFTMintEmbed(nft).build()).queue();
    }

    public void postNFTBuy(NFT nft, String buyerId) {
        this.transactionHistoryChannel.sendMessage(EmbedUtils.getNFTSaleEmbed(nft, buyerId).build()).queue();
    }

    public void postNFTTrade(NFT nft, String payerId, String payeeId) {
        this.transactionHistoryChannel.sendMessage(EmbedUtils.getNFTTransferEmbed(nft.getName(), payerId, payeeId).build()).queue();
    }
    //endregion
}
