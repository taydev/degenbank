package dev.ults.degenbank.obj;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class NFT {

    @BsonId
    private final String name;
    @BsonProperty("creator_id")
    private final String creatorID;
    @BsonProperty("url")
    private final String url;

    @BsonProperty("price")
    private long price;
    @BsonProperty("for_sale")
    private boolean forSale;

    @BsonCreator
    public NFT(@BsonId String name, @BsonProperty("creator_id") String creatorID,
            @BsonProperty("url") String url, @BsonProperty("price") long price, @BsonProperty("for_sale") boolean forSale) {
        this.name = name;
        this.creatorID = creatorID;
        this.url = url;
        this.price = price;
        this.forSale = forSale;
    }

    @BsonId
    public String getName() {
        return this.name;
    }

    @BsonProperty("creator_id")
    public String getCreatorID() {
        return this.creatorID;
    }

    @BsonProperty("url")
    public String getUrl() {
        return this.url;
    }

    @BsonProperty("price")
    public long getPrice() {
        return this.price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    @BsonProperty("for_sale")
    public boolean isForSale() {
        return this.forSale;
    }

    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }
}
