package com.noveflix.app.network;

import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.Episode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Busca episódios direto no servidor NoveFlix.
 * Fallback para dados mock se o servidor estiver vazio ou offline.
 */
public class ServerRepository {

    // URL do servidor na SquareCloud — atualize se mudar
    public static final String SERVER_BASE_URL = "https://relaxeinov.squareweb.app/";

    public interface FeedCallback {
        void onSuccess(List<Episode> episodes, boolean fromServer);
        void onError(String message);
    }

    // ── Interface Retrofit do servidor ──────────────────────
    interface NoveFlixApi {
        @GET("api/feed")
        Call<FeedResponse> getFeed(
                @Query("page")     int page,
                @Query("per_page") int perPage
        );
    }

    // ── Modelo de resposta do servidor ──────────────────────
    static class FeedResponse {
        public int           total;
        public int           page;
        public List<EpisodeDto> episodes;
    }

    static class EpisodeDto {
        public String id;
        public String novelaId;
        public String novelaTitle;
        public String title;
        public String description;
        public String thumbnailUrl;
        public String videoUrl;
        public int    episodeNumber;
        public int    durationSeconds;
        public int    coinCost;
        public int    likeCount;
        public int    viewCount;
        public String countryFlag;
        public String countryCode;
    }

    // ── Cliente Retrofit separado (não usa o TMDB base URL) ─
    private final NoveFlixApi api;

    public ServerRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(NoveFlixApi.class);
    }

    /**
     * Busca o feed do servidor NoveFlix.
     * Se o servidor retornar lista vazia ou falhar, chama tmdbFallback.
     */
    public void loadFeed(int perPage, final FeedCallback callback) {
        api.getFeed(1, perPage).enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(Call<FeedResponse> call, Response<FeedResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().episodes != null
                        && !response.body().episodes.isEmpty()) {
                    callback.onSuccess(mapAll(response.body().episodes), true);
                } else {
                    // Servidor vazio → TMDB
                    fallbackToTmdb(callback);
                }
            }

            @Override
            public void onFailure(Call<FeedResponse> call, Throwable t) {
                // Servidor offline → TMDB
                fallbackToTmdb(callback);
            }
        });
    }

    private void fallbackToTmdb(FeedCallback callback) {
        callback.onSuccess(MockDataProvider.getFeedEpisodes(), false);
    }

    private List<Episode> mapAll(List<EpisodeDto> dtos) {
        List<Episode> result = new ArrayList<>();
        for (EpisodeDto dto : dtos) {
            result.add(new Episode(
                    dto.id,
                    dto.novelaId,
                    dto.novelaTitle,
                    dto.title,
                    dto.description != null ? dto.description : "",
                    dto.thumbnailUrl != null ? dto.thumbnailUrl : "",
                    dto.videoUrl != null ? dto.videoUrl : "",
                    dto.episodeNumber,
                    dto.durationSeconds,
                    dto.coinCost,
                    dto.likeCount,
                    dto.viewCount,
                    dto.countryFlag != null ? dto.countryFlag : "",
                    dto.countryCode != null ? dto.countryCode : ""
            ));
        }
        return result;
    }
}
