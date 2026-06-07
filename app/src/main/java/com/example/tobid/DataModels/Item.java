package com.example.tobid.DataModels;

import java.io.Serializable;

public class Item implements Serializable {
    private final int itemId;
    private String storagePathToImage;
    private final String sellerUID;
    private  String itemDescription;

    // TODO: Should probaby switch to enum for clearer readability
    private String status;
    private final String category;

    public Item(int itemId, String storagePathToImage, String sellerUID, String itemDescription, String status, String category) {
        this.itemId = itemId;
        this.storagePathToImage = storagePathToImage;
        this.sellerUID = sellerUID;
        this.itemDescription = itemDescription;
        this.status = status;
        this.category = category;
    }

    public int getItemId() {
        return itemId;
    }

    public String getStoragePathToImage() {
        return storagePathToImage;
    }

    public void setStoragePathToImage(String storagePathToImage) {
        this.storagePathToImage = storagePathToImage;
    }

    public String getSellerUID() {
        return sellerUID;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }
}
