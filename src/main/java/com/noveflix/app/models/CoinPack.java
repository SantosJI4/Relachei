package com.noveflix.app.models;

public class CoinPack {
    private String id;
    private String name;
    private int coins;
    private int bonusCoins;
    private String price;
    private boolean isPopular;
    private int savingsPercent;

    public CoinPack() {}

    public CoinPack(String id, String name, int coins, int bonusCoins,
                    String price, boolean isPopular, int savingsPercent) {
        this.id = id;
        this.name = name;
        this.coins = coins;
        this.bonusCoins = bonusCoins;
        this.price = price;
        this.isPopular = isPopular;
        this.savingsPercent = savingsPercent;
    }

    public int getTotalCoins() {
        return coins + bonusCoins;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public int getBonusCoins() { return bonusCoins; }
    public void setBonusCoins(int bonusCoins) { this.bonusCoins = bonusCoins; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public boolean isPopular() { return isPopular; }
    public void setPopular(boolean popular) { isPopular = popular; }

    public int getSavingsPercent() { return savingsPercent; }
    public void setSavingsPercent(int savingsPercent) { this.savingsPercent = savingsPercent; }
}
