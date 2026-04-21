package com.noveflix.app.models;

import java.util.List;

public class Novela {
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String countryFlag;
    private String countryCode;
    private List<Episode> episodes;

    public Novela(String id, String title, String description, String thumbnailUrl,
                  String countryFlag, String countryCode, List<Episode> episodes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.countryFlag = countryFlag;
        this.countryCode = countryCode;
        this.episodes = episodes;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getCountryFlag() { return countryFlag; }
    public String getCountryCode() { return countryCode; }
    public List<Episode> getEpisodes() { return episodes; }
    public int getEpisodeCount() { return episodes != null ? episodes.size() : 0; }
}
