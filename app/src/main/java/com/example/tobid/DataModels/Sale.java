package com.example.tobid.DataModels;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Sale implements Serializable {
    private final Item item;
    private final String startDate;
    private final String endDate;
    private final float startingPrice;
    private float highestOfferedBid;
    private boolean isMaximumPrice;
    private final float maximumPrice;
    private String leadingBidderId;
    private int bidsMade;

    public Sale(Item item, String startDate, String endDate, float startingPrice, boolean isMaximumPrice, float maximumPrice) {
        this.item = item;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startingPrice = startingPrice;
        this.isMaximumPrice = isMaximumPrice;
        this.maximumPrice = maximumPrice;

        // Default initialization
        this.highestOfferedBid = 0;
        this.leadingBidderId = null;
        this.bidsMade = 0;
    }

    public Item getItem() {
        return item;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public float getStartingPrice() {
        return startingPrice;
    }

    public float getHighestOfferedBid() {
        return highestOfferedBid;
    }

    public void setHighestOfferedBid(float highestOfferedBid) {
        this.highestOfferedBid = highestOfferedBid;
    }

    public float getMaximumPrice() {
        return this.maximumPrice;
    }

    public boolean isHasMaximumPrice() {
        return this.isMaximumPrice;
    }

    public void setHasMaximumPrice(boolean isMaximumPrice) {
        this.isMaximumPrice = isMaximumPrice;
    }

    public String getLeadingBidderId() {
        return leadingBidderId;
    }

    public void setLeadingBidderId(String leadingBidderId) {
        this.leadingBidderId = leadingBidderId;
    }

    public int getBidsMade() {
        return bidsMade;
    }

    public void setBidsMade(int bidsMade) {
        this.bidsMade = bidsMade;
    }
}
