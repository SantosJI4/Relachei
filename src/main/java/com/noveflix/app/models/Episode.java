package com.noveflix.app.models;

public class Episode {
    private String id;
    private String novelaId;
    private String novelaTitle;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private int episodeNumber;
    private int durationSeconds;
    private int coinCost;        // 0 = grátis, 2 = pago
    private int likeCount;
    private int viewCount;
    private boolean liked;
    private boolean isFirst;     // primeiro episódio sempre grátis
    private String countryFlag;  // emoji da bandeira
    private String countryCode;  // "JP", "TR", "MX", "KR"

    public Episode() {}

    public Episode(String id, String novelaId, String novelaTitle, String title,
                   String description, String thumbnailUrl, String videoUrl,
                   int episodeNumber, int durationSeconds, int coinCost,
                   int likeCount, int viewCount, String countryFlag, String countryCode) {
        this.id = id;
        this.novelaId = novelaId;
        this.novelaTitle = novelaTitle;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.episodeNumber = episodeNumber;
        this.durationSeconds = durationSeconds;
        this.coinCost = coinCost;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.liked = false;
        this.isFirst = (episodeNumber == 1);
        this.countryFlag = countryFlag;
        this.countryCode = countryCode;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNovelaId() { return novelaId; }
    public void setNovelaId(String novelaId) { this.novelaId = novelaId; }

    public String getNovelaTitle() { return novelaTitle; }
    public void setNovelaTitle(String novelaTitle) { this.novelaTitle = novelaTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public int getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(int episodeNumber) { this.episodeNumber = episodeNumber; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getCoinCost() { return coinCost; }
    public void setCoinCost(int coinCost) { this.coinCost = coinCost; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isFirst() { return isFirst; }
    public void setFirst(boolean first) { isFirst = first; }

    public String getCountryFlag() { return countryFlag; }
    public void setCountryFlag(String countryFlag) { this.countryFlag = countryFlag; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public boolean isFree() {
        return isFirst || coinCost == 0;
    }

    public String getFormattedDuration() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedLikes() {
        if (likeCount >= 1000000) {
            return String.format("%.1fM", likeCount / 1000000f);
        } else if (likeCount >= 1000) {
            return String.format("%.1fK", likeCount / 1000f);
        }
        return String.valueOf(likeCount);
    }
}
