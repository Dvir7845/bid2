package com.example.tobid.DataModels;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Sales implements Serializable {
    private final int itemId;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final float startingPrice;
    private float highestOfferedBid;
    private final float maximumPrice;
    private String leadingBidderId;
    private int bidsMade;

    public Sales(int itemId, LocalDateTime startDate, LocalDateTime endDate, float startingPrice, float maximumPrice) {
        this.itemId = itemId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startingPrice = startingPrice;
        this.maximumPrice = maximumPrice;

        // Default initialization
        this.highestOfferedBid = 0;
        this.leadingBidderId = null;
        this.bidsMade = 0;
    }

    public int getItemId() {
        return itemId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
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
        return maximumPrice;
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
