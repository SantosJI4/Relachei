package com.noveflix.app.network;

import com.noveflix.app.BuildConfig;
import com.noveflix.app.models.Episode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TmdbRepository {

    private static final String SORT_POPULAR  = "popularity.desc";
    private static final String LANGUAGE_PTBR = "pt-BR";

    private final TmdbApiService api;
    private final String         apiKey;

    public interface FeedCallback {
        void onSuccess(List<Episode> episodes);
        void onError(String message);
    }

    // Substitui java.util.function.Consumer que exige API 24+ (minSdk é 21)
    interface EpisodesCallback {
        void accept(List<Episode> episodes);
    }

    public TmdbRepository() {
        api    = RetrofitClient.getInstance().getApi();
        apiKey = BuildConfig.TMDB_API_KEY;
    }

    public void loadFeed(final int showsPerCountry, final int episodesPerShow,
                         final FeedCallback callback) {
        final String[]      countries = {"JP", "KR", "TR", "MX", "CN"};
        final List<Episode> result    = new ArrayList<>();
        final AtomicInteger pending   = new AtomicInteger(countries.length);

        for (final String code : countries) {
            api.discoverTv(apiKey, LANGUAGE_PTBR, code, SORT_POPULAR, 1, "")
               .enqueue(new Callback<TmdbModels.TvDiscoverResponse>() {

                   @Override
                   public void onResponse(Call<TmdbModels.TvDiscoverResponse> call,
                                          Response<TmdbModels.TvDiscoverResponse> response) {
                       if (response.isSuccessful() && response.body() != null
                               && response.body().results != null
                               && !response.body().results.isEmpty()) {

                           final List<TmdbModels.TvShow> shows = response.body().results;
                           final int limit = Math.min(shows.size(), showsPerCountry);
                           final AtomicInteger seasonPending = new AtomicInteger(limit);

                           for (int i = 0; i < limit; i++) {
                               final TmdbModels.TvShow show = shows.get(i);
                               fetchSeasonEpisodes(show, code, episodesPerShow,
                                       new EpisodesCallback() {
                                           @Override
                                           public void accept(List<Episode> eps) {
                                               synchronized (result) {
                                                   result.addAll(eps);
                                               }
                                               if (seasonPending.decrementAndGet() == 0) {
                                                   if (pending.decrementAndGet() == 0) {
                                                       callback.onSuccess(result);
                                                   }
                                               }
                                           }
                                       });
                           }
                       } else {
                           if (pending.decrementAndGet() == 0) {
                               callback.onSuccess(result);
                           }
                       }
                   }

                   @Override
                   public void onFailure(Call<TmdbModels.TvDiscoverResponse> call, Throwable t) {
                       if (pending.decrementAndGet() == 0) {
                           if (result.isEmpty()) {
                               callback.onError("Erro de rede: " + t.getMessage());
                           } else {
                               callback.onSuccess(result);
                           }
                       }
                   }
               });
        }
    }

    private void fetchSeasonEpisodes(final TmdbModels.TvShow show, final String countryCode,
                                     final int maxEpisodes, final EpisodesCallback onDone) {
        api.getSeason(show.id, 1, apiKey, LANGUAGE_PTBR)
           .enqueue(new Callback<TmdbModels.TvSeasonResponse>() {

               @Override
               public void onResponse(Call<TmdbModels.TvSeasonResponse> call,
                                      Response<TmdbModels.TvSeasonResponse> response) {
                   List<Episode> eps = new ArrayList<>();
                   if (response.isSuccessful() && response.body() != null
                           && response.body().episodes != null) {
                       List<TmdbModels.TvEpisode> tmdbEps = response.body().episodes;
                       int limit = Math.min(tmdbEps.size(), maxEpisodes);
                       for (int i = 0; i < limit; i++) {
                           eps.add(mapToEpisode(tmdbEps.get(i), show, countryCode));
                       }
                   }
                   onDone.accept(eps);
               }

               @Override
               public void onFailure(Call<TmdbModels.TvSeasonResponse> call, Throwable t) {
                   // Retorna lista vazia; o show simplesmente não aparece no feed
                   onDone.accept(new ArrayList<>());
               }
           });
    }

    /**
     * Converte TvEpisode (TMDB) + TvShow → Episode (modelo do app).
     */
    private Episode mapToEpisode(TmdbModels.TvEpisode tmdb,
                                  TmdbModels.TvShow   show,
                                  String countryCode) {
        String imgBase = BuildConfig.TMDB_IMG_BASE;

        // Thumbnail: usa still do episódio; se vazio usa poster da série
        String thumbnail = (tmdb.stillPath != null && !tmdb.stillPath.isEmpty())
                ? imgBase + tmdb.stillPath
                : (show.posterPath != null ? imgBase + show.posterPath : "");

        // Episódio 1 sempre gratuito; demais custam 2 moedas
        int coinCost = (tmdb.episodeNumber == 1) ? 0 : 2;

        int durationSec = (tmdb.runtime > 0) ? tmdb.runtime * 60 : 1440; // default 24 min

        int likes = (int) (show.voteAverage * 1200 + show.voteCount * 0.1);
        int views = (int) (show.popularity  * 800);

        String flag = countryFlag(countryCode);

        return new Episode(
                countryCode.toLowerCase() + "_" + show.id + "_ep" + tmdb.episodeNumber,
                String.valueOf(show.id),
                show.name != null ? show.name : show.originalName,
                tmdb.name != null ? tmdb.name : "Episódio " + tmdb.episodeNumber,
                tmdb.overview != null && !tmdb.overview.isEmpty()
                        ? tmdb.overview
                        : show.overview != null ? show.overview : "",
                thumbnail,
                "",           // videoUrl — integrar serviço de streaming
                tmdb.episodeNumber,
                durationSec,
                coinCost,
                likes,
                views,
                flag,
                countryCode
        );
    }

    private String countryFlag(String code) {
        switch (code) {
            case "JP": return "🇯🇵";
            case "KR": return "🇰🇷";
            case "TR": return "🇹🇷";
            case "MX": return "🇲🇽";
            case "CN": return "🇨🇳";
            default:   return "🌍";
        }
    }
}
