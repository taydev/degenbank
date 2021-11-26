package dev.ults.degenbank.obj;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Degen {

    @BsonId
    private final String id;
    @BsonProperty("degen_coin_balance")
    private long degenCoinBalance;
    @BsonProperty("owned_tokens")
    private final List<String> ownedTokens;

    public Degen(String id) {
        this(id, 0, new ArrayList<>());
    }

    @BsonCreator
    public Degen(@BsonId String id, @BsonProperty("degen_coin_balance") long degenCoinBalance,
            @BsonProperty("owned_tokens") List<String> ownedTokens) {
        this.id = id;
        this.degenCoinBalance = degenCoinBalance;
        this.ownedTokens = ownedTokens;
    }

    @BsonId
    public String getId() {
        return this.id;
    }

    @BsonProperty("degen_coin_balance")
    public long getDegenCoinBalance() {
        return this.degenCoinBalance;
    }

    public void setDegenCoinBalance(long degenCoinBalance) {
        this.degenCoinBalance = degenCoinBalance;
    }

    public void addDegenCoin(long quantity) {
        this.degenCoinBalance += quantity;
    }

    @BsonIgnore
    public boolean removeDegenCoin(long quantity) {
        long balance = this.getDegenCoinBalance();
        if (balance >= quantity) {
            long newBalance = balance - quantity;
            this.setDegenCoinBalance(newBalance);
            return true;
        } else {
            return false;
        }
    }

    @BsonProperty("owned_tokens")
    public List<String> getOwnedTokens() {
        return this.ownedTokens;
    }

    public void addToken(String name) {
        this.getOwnedTokens().add(name);
    }

    public void removeToken(String name) {
        this.getOwnedTokens().remove(name);
    }

}
