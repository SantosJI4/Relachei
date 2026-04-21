package com.noveflix.app.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Modelos TMDB — mapeiam o JSON da API diretamente.
 * Referência: https://developers.themoviedb.org/3
 */
public class TmdbModels {

    // ── Resposta de /discover/tv ────────────────────────────
    public static class TvDiscoverResponse {
        @SerializedName("page")         public int page;
        @SerializedName("results")      public List<TvShow> results;
        @SerializedName("total_pages")  public int totalPages;
        @SerializedName("total_results") public int totalResults;
    }

    // ── Série / Show ────────────────────────────────────────
    public static class TvShow {
        @SerializedName("id")               public int id;
        @SerializedName("name")             public String name;
        @SerializedName("original_name")    public String originalName;
        @SerializedName("overview")         public String overview;
        @SerializedName("poster_path")      public String posterPath;
        @SerializedName("backdrop_path")    public String backdropPath;
        @SerializedName("origin_country")   public List<String> originCountry;
        @SerializedName("vote_average")     public double voteAverage;
        @SerializedName("vote_count")       public int voteCount;
        @SerializedName("popularity")       public double popularity;
        @SerializedName("first_air_date")   public String firstAirDate;
        @SerializedName("genre_ids")        public List<Integer> genreIds;
        @SerializedName("number_of_seasons") public int numberOfSeasons;
        @SerializedName("number_of_episodes") public int numberOfEpisodes;
    }

    // ── Temporada (resposta de /tv/{id}/season/{n}) ─────────
    public static class TvSeasonResponse {
        @SerializedName("id")           public int id;
        @SerializedName("name")         public String name;
        @SerializedName("overview")     public String overview;
        @SerializedName("poster_path")  public String posterPath;
        @SerializedName("season_number") public int seasonNumber;
        @SerializedName("episodes")     public List<TvEpisode> episodes;
    }

    // ── Episódio ────────────────────────────────────────────
    public static class TvEpisode {
        @SerializedName("id")               public int id;
        @SerializedName("episode_number")   public int episodeNumber;
        @SerializedName("season_number")    public int seasonNumber;
        @SerializedName("name")             public String name;
        @SerializedName("overview")         public String overview;
        @SerializedName("still_path")       public String stillPath;
        @SerializedName("runtime")          public int runtime;       // minutos
        @SerializedName("vote_average")     public double voteAverage;
        @SerializedName("vote_count")       public int voteCount;
        @SerializedName("air_date")         public String airDate;
        @SerializedName("show_id")          public int showId;
    }
}
