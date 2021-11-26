package dev.ults.degenbank.obj;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class Transaction {

    @BsonId
    private int transactionId;
    @BsonProperty("payer_id")
    private final String payerId;
    @BsonProperty("payee_id")
    private final String payeeId;
    @BsonProperty("balance")
    private final long balance;
    @BsonProperty("note")
    private final String note;

    public Transaction(String payerId, String payeeId, long balance, String note) {
        this.transactionId = -333;
        this.payerId = payerId;
        this.payeeId = payeeId;
        this.balance = balance;
        this.note = note;
    }

    @BsonCreator
    public Transaction(@BsonId int transactionId, @BsonProperty("payer_id") String payerId,
            @BsonProperty("payee_id") String payeeId, @BsonProperty("balance") long balance,
            @BsonProperty("note") String note) {
        this.transactionId = transactionId;
        this.payerId = payerId;
        this.payeeId = payeeId;
        this.balance = balance;
        this.note = note;
    }

    @BsonId
    public int getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    @BsonProperty("payer_id")
    public String getPayerId() {
        return this.payerId;
    }

    @BsonProperty("payee_id")
    public String getPayeeId() {
        return this.payeeId;
    }

    @BsonProperty("balance")
    public long getBalance() {
        return this.balance;
    }

    @BsonProperty("note")
    public String getNote() {
        return this.note;
    }
}
