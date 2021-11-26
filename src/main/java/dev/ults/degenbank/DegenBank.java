package dev.ults.degenbank;

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
import java.awt.Color;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DegenBank {

    public static final DegenBank INSTANCE = new DegenBank();
    public static final Logger LOGGER = LoggerFactory.getLogger(DegenBank.class);

    private final MongoClient MONGO_CLIENT;
    private final FindOneAndReplaceOptions UPSERT_OPTS;
    private final CodecRegistry CODEC_REGISTRY;

    // bot variables
    private String token;
    private String prefix;
    private JDA client;
    private Set<ICommand> commands;

    // caching
    private Set<Degen> cachedDegens;
    private Set<NFT> cachedNFTs;

    // transactions
    private boolean acceptingTransactions;
    private MessageChannel transactionHistoryChannel;
    private List<Transaction> pendingTransactionLog;
    private Thread transactionProcessingThread;

    public DegenBank() {
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

    private void initialise() {
        if (!this.parseConfiguration()) {
            // this should probably be migrated to a file
            // just for ease of use
            // hm
            LOGGER.info("Configuration not detected. Default configuration has been inserted. Do your thing, chief.");
            System.exit(69);
        }
        this.connectToDiscord();
    }

    private MongoClient getMongoClient() {
        return this.MONGO_CLIENT;
    }

    private MongoDatabase getDatabase() {
        return this.getMongoClient().getDatabase("degen_bank").withCodecRegistry(this.CODEC_REGISTRY);
    }

    // what am I doing? I'm putting the config in Mango.
    // why? because I'm too lazy to parse a file rn
    // deal w/ it, me in about 3 days
    // update from me in 3 days: yeah true I need to fix this
    private MongoCollection<Document> getConfiguration() {
        return this.getDatabase().getCollection("config");
    }

    // also I love how this doesn't even use any actual logic lmao
    // might be worth integrating an environment variable to dictate which db to use
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
        // \/ REORDER THIS MESS \/
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
        // ^ REORDER THIS MESS ^
        this.transactionHistoryChannel = this.getClient().getTextChannelById("912892124326944788");
        this.pendingTransactionLog = new CopyOnWriteArrayList<>();
        this.startTransactionProcessingThread();

        this.cachedDegens = new HashSet<>();
        this.cachedNFTs = new HashSet<>();
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

    public Set<Degen> getCachedDegens() {
        return this.cachedDegens;
    }

    public MongoCollection<Degen> getDegens() {
        return this.getDatabase().getCollection("degens", Degen.class);
    }

    // could probably just use caffeine for caching purposes but I'd rather do it by hand
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

    public void storeDegen(Degen degen) {
        // this should work, theoretically
        this.getDegens().findOneAndReplace(Filters.eq("_id", degen.getId()), degen, this.UPSERT_OPTS);
    }

    public MongoCollection<NFT> getNFTs() {
        return this.getDatabase().getCollection("nfts", NFT.class);
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

    public void storeNFT(NFT nft) {
        this.getNFTs().findOneAndReplace(Filters.eq("_id", nft.getName()), nft, this.UPSERT_OPTS);
    }

    public MongoCollection<Transaction> getTransactions() {
        return this.getDatabase().getCollection("transactions", Transaction.class);
    }

    public void insertTransaction(String payerId, String payeeId, long balance, String note) {
        this.pendingTransactionLog.add(new Transaction(payerId, payeeId, balance, note));
    }

    // this entire function is useless
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

    // TODO: clean
    private void startTransactionProcessingThread() {
        this.transactionProcessingThread = new Thread(() -> {
            while (true) {
                if (this.pendingTransactionLog.size() > 0) {
                    Transaction transaction = this.pendingTransactionLog.get(0);
                    transaction.setTransactionId(this.getNextTransactionId());
                    this.getTransactions().insertOne(transaction);
                    this.transactionHistoryChannel.sendMessage(new EmbedBuilder()
                            .setColor(Color.ORANGE)
                            .setTitle("Transaction #" + DegenUtils.getFormattedBalance(transaction.getTransactionId()))
                            .addField("Payer", String.format("<@%s>", transaction.getPayerId()), true)
                            .addField("Payee", String.format("<@%s>", transaction.getPayeeId()), true)
                            .addField("Value", DegenUtils.getDisplayBalance(transaction.getValue()), true)
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
        this.transactionHistoryChannel.sendMessage(EmbedUtils.getNFTMintEmbed(nft).build()).queue();
    }

    public void postNFTBuy(NFT nft, String buyerId) {
        this.transactionHistoryChannel.sendMessage(EmbedUtils.getNFTSaleEmbed(nft, buyerId).build()).queue();
    }

    public void postNFTTrade(NFT nft, String payerId, String payeeId) {
        this.transactionHistoryChannel.sendMessage(EmbedUtils.getNFTTransferEmbed(nft.getName(), payerId, payeeId).build()).queue();
    }
}
