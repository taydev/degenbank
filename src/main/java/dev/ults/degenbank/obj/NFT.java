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
    @BsonProperty("initial_price")
    private final long initialPrice;
    @BsonProperty("for_sale")
    private boolean forSale;
    @BsonProperty("last_sale_price")
    private long lastSalePrice;
    @BsonProperty("sale_price")
    private long salePrice;

    @BsonCreator
    public NFT(@BsonId String name, @BsonProperty("creator_id") String creatorID,
            @BsonProperty("url") String url, @BsonProperty("initial_price") long initialPrice,
            @BsonProperty("for_sale") boolean forSale, @BsonProperty("sale_price") long salePrice,
            @BsonProperty("last_sale_price") long lastSalePrice) {
        this.name = name;
        this.creatorID = creatorID;
        this.url = url;
        this.initialPrice = initialPrice;
        this.forSale = forSale;
        this.salePrice = salePrice;
        this.lastSalePrice = lastSalePrice;
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

    @BsonProperty("initial_price")
    public long getInitialPrice() {
        return this.initialPrice;
    }

    @BsonProperty("for_sale")
    public boolean isForSale() {
        return this.forSale;
    }

    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }

    @BsonProperty("sale_price")
    public long getSalePrice() {
        return this.salePrice;
    }

    public void setSalePrice(long salePrice) {
        this.salePrice = salePrice;
    }

    @BsonProperty("last_sale_price")
    public long getLastSalePrice() {
        return this.lastSalePrice;
    }

    public void setLastSalePrice(long lastSalePrice) {
        this.lastSalePrice = lastSalePrice;
    }
}
