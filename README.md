In case category table is deleted from db:
        ArrayList<String> categories = new ArrayList<>();
        categories.add("CLOTHING_AND_FASHION");
        categories.add("SPORTS_AND_OUTDOORS");
        categories.add("HOME_AND_GARDEN");
        categories.add("TOOLS_AND_HARDWARE");
        categories.add("ART_AND_COLLECTIBLES");
        categories.add("TOYS_AND_HOBBIES");
        categories.add("BEAUTY_AND_HEALTH");
        categories.add("JEWELRY_AND_WATCHES");
        categories.add("ELECTRONICS");
        categories.add("VEHICLES");
        categories.add("REAL_ESTATE");
        categories.add("OTHER");
        database.getReference().child("Categories").setValue(categories);
