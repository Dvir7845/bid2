package com.example.tobid.DataModels;

import java.io.Serializable;

public class Item implements Serializable {
    private String itemId;
    private String itemName;
    private String storagePathToImg1, storagePathToImg2, storagePathToImg3;
    private String sellerUID;
    private String itemDescription;
    private String category;

    public Item(String itemName, String itemId, String itemDescription, String category, String sellerUID, String storagePathToImg1, String storagePathToImg2, String storagePathToImg3) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.storagePathToImg1 = storagePathToImg1;
        this.storagePathToImg2 = storagePathToImg2;
        this.storagePathToImg3 = storagePathToImg3;
        this.sellerUID = sellerUID;
        this.itemDescription = itemDescription;
        this.category = category;
    }
    public Item() {
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemId() {
        return itemId;
    }

    public String getStoragePathToImg1() {
        return storagePathToImg1;
    }

    public void setStoragePathToImg1(String storagePathToImg1) {
        this.storagePathToImg1 = storagePathToImg1;
    }

    public String getStoragePathToImg2() {
        return storagePathToImg2;
    }

    public void setStoragePathToImg2(String storagePathToImg2) {
        this.storagePathToImg2 = storagePathToImg2;
    }

    public String getStoragePathToImg3() {
        return storagePathToImg3;
    }

    public void setStoragePathToImg3(String storagePathToImg3) {
        this.storagePathToImg3 = storagePathToImg3;
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

    public String getCategory() {
        return category;
    }
}

