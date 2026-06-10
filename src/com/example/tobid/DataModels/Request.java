package com.example.tobid.DataModels;

import java.io.Serializable;

public class Request implements Serializable {
    private RequestAction action;  // "PLACE_BID", "BUY_NOW", "AUTO_BID"
    private String saleId;
    private String userId;
    private float amount;

    public Request(RequestAction action, String saleId, String userId, float amount) {
        this.action = action;
        this.saleId = saleId;
        this.userId = userId;
        this.amount = amount;
    }
    public enum RequestAction {
        PLACE_BID,
        BUY_NOW,
        AUTO_BID,
        GET_SALE,
        GET_ALL_SALES,
        CREATE_SALE,
        LOGIN,
        REGISTER
    }

    public RequestAction getAction() {
        return action;
    }

    public String getSaleId() {
        return saleId;
    }

    

    public String getUserId() {
        return userId;
    }

    public float getAmount() {
        return amount;
    }
}


