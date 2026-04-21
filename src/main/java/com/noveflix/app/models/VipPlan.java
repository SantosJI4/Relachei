package com.noveflix.app.models;

import java.util.List;

public class VipPlan {
    public static final String TYPE_PACK     = "pack";
    public static final String TYPE_FLASH    = "flash";
    public static final String TYPE_WEEKLY   = "weekly";
    public static final String TYPE_MONTHLY  = "monthly";
    public static final String TYPE_ANNUAL   = "annual";

    private String id;
    private String type;
    private String name;
    private String subtitle;
    private String price;
    private String priceDetail;
    private List<String> benefits;
    private int bonusCoins;
    private boolean isPopular;
    private boolean isBestValue;
    private long durationMillis;   // -1 para pack de novelas
    private int novelaPacks;       // apenas para TYPE_PACK

    public VipPlan() {}

    public VipPlan(String id, String type, String name, String subtitle,
                   String price, String priceDetail, List<String> benefits,
                   int bonusCoins, boolean isPopular, boolean isBestValue,
                   long durationMillis, int novelaPacks) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.subtitle = subtitle;
        this.price = price;
        this.priceDetail = priceDetail;
        this.benefits = benefits;
        this.bonusCoins = bonusCoins;
        this.isPopular = isPopular;
        this.isBestValue = isBestValue;
        this.durationMillis = durationMillis;
        this.novelaPacks = novelaPacks;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getPriceDetail() { return priceDetail; }
    public void setPriceDetail(String priceDetail) { this.priceDetail = priceDetail; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    public int getBonusCoins() { return bonusCoins; }
    public void setBonusCoins(int bonusCoins) { this.bonusCoins = bonusCoins; }

    public boolean isPopular() { return isPopular; }
    public void setPopular(boolean popular) { isPopular = popular; }

    public boolean isBestValue() { return isBestValue; }
    public void setBestValue(boolean bestValue) { isBestValue = bestValue; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public int getNovelaPacks() { return novelaPacks; }
    public void setNovelaPacks(int novelaPacks) { this.novelaPacks = novelaPacks; }
}
