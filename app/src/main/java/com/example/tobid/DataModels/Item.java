package com.example.tobid.DataModels;

import java.io.Serializable;

public class Item implements Serializable {
    private  int itemId;
    private String storagePathToImage;
    private  String sellerUID;
    private  String itemDescription;

    // TODO: Should probaby switch to enum for clearer readability
    //TODO: maybe switch to boolean for status?
    private String status;
    private Category category;
    public enum Category {
        ELECTRONICS("Electronics"),
        CLOTHING_AND_FASHION("Clothing & Fashion"),
        SPORTS_AND_OUTDOORS("Sports & Outdoors"),
        HOME_AND_GARDEN("Home & Garden"),
        TOOLS_AND_HARDWARE("Tools & Hardware"),
        ART_AND_COLLECTIBLES("Art & Collectibles"),
        TOYS_AND_HOBBIES("Toys & Hobbies"),
        BEAUTY_AND_HEALTH("Beauty & Health"),
        JEWELRY_AND_WATCHES("Jewelry & Watches"),
        VEHICLES("Vehicles"),
        REAL_ESTATE("Real Estate"),
        OTHER("Other");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }

    public Item(int itemId, String storagePathToImage, String sellerUID, String itemDescription, String status, Category category) {
        this.itemId = itemId;
        this.storagePathToImage = storagePathToImage;
        this.sellerUID = sellerUID;
        this.itemDescription = itemDescription;
        this.status = status;
        this.category = category;
    }
    public Item() {
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
        return category.getDisplayName();
    }
}
