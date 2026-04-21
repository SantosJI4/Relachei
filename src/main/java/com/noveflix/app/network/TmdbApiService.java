package com.noveflix.app.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface Retrofit para a API TMDB v3.
 * Base URL: https://api.themoviedb.org/3/
 */
public interface TmdbApiService {

    /**
     * Descobre séries populares por país de origem.
     * Exemplo: with_origin_country=JP → dramas japoneses
     */
    @GET("discover/tv")
    Call<TmdbModels.TvDiscoverResponse> discoverTv(
            @Query("api_key")              String apiKey,
            @Query("language")             String language,
            @Query("with_origin_country")  String countryCode,
            @Query("sort_by")              String sortBy,
            @Query("page")                 int    page,
            @Query("with_genres")          String withGenres   // 10766 = soap opera, vazio = todos
    );

    /**
     * Detalhes completos de uma série (inclui number_of_seasons).
     */
    @GET("tv/{series_id}")
    Call<TmdbModels.TvShow> getTvDetail(
            @Path("series_id") int    seriesId,
            @Query("api_key")  String apiKey,
            @Query("language") String language
    );

    /**
     * Lista todos os episódios de uma temporada.
     */
    @GET("tv/{series_id}/season/{season_number}")
    Call<TmdbModels.TvSeasonResponse> getSeason(
            @Path("series_id")      int    seriesId,
            @Path("season_number")  int    seasonNumber,
            @Query("api_key")       String apiKey,
            @Query("language")      String language
    );
}
