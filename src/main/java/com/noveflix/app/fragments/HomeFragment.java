package com.noveflix.app.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;

import com.noveflix.app.AdWebViewActivity;
import com.noveflix.app.MainActivity;
import com.noveflix.app.R;
import com.noveflix.app.adapters.FeedAdapter;
import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.Episode;
import com.noveflix.app.network.ServerRepository;
import com.noveflix.app.utils.PrefsManager;

import java.util.List;

public class HomeFragment extends Fragment implements FeedAdapter.OnEpisodeClickListener {

    private static final String AD_URL = "https://relaxeinov.squareweb.app/ad";
    private static final int    AD_EVERY_N_EPISODES = 2;

    private int              episodesSinceLastAd = 0;
    private Episode          pendingEpisode      = null;

    private ListView         feedListView;
    private FeedAdapter      adapter;
    private TextView         tvCoinBalance;
    private PrefsManager     prefs;
    private List<Episode>    episodes;
    private ServerRepository serverRepository;
    private int              currentPlayingPosition = -1;
    // Cache: posição salva ao sair, restaurada ao voltar
    private int              lastKnownPosition      = 0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs         = PrefsManager.getInstance(getActivity());
        tvCoinBalance = (TextView) view.findViewById(R.id.tv_coin_balance_home);
        feedListView  = (ListView) view.findViewById(R.id.feed_list_view);

        // Rolagem mais suave: desabilita over-scroll e fling excessivo
        feedListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        feedListView.setFriction(0.09f);

        serverRepository = new ServerRepository();
        episodes = MockDataProvider.getFeedEpisodes();
        adapter  = new FeedAdapter(getActivity(), episodes, this);
        feedListView.setAdapter(adapter);

        // === TIKTOK SCROLL LISTENER: snap + auto-play ao parar ===
        feedListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView v, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    snapAndPlay();
                }
            }
            public void onScroll(AbsListView v, int first, int count, int total) {}
        });

        updateCoinDisplay();
        loadFeed();
    }

    public void onResume() {
        super.onResume();
        updateCoinDisplay();
        feedListView.post(new Runnable() {
            public void run() {
                if (lastKnownPosition > 0 && lastKnownPosition < episodes.size()) {
                    // Restaura posição anterior (cache)
                    feedListView.smoothScrollToPositionFromTop(lastKnownPosition, 0, 1);
                    feedListView.post(new Runnable() {
                        public void run() { playAt(lastKnownPosition); }
                    });
                } else {
                    snapAndPlay();
                }
            }
        });
    }

    public void onPause() {
        super.onPause();
        // Salva posição antes de parar (cache)
        if (currentPlayingPosition >= 0) lastKnownPosition = currentPlayingPosition;
        stopAllVideos();
    }

    private void updateCoinDisplay() {
        if (tvCoinBalance != null) {
            tvCoinBalance.setText("" + prefs.getCoins() + " moedas");
        }
    }

    // Snapa para o item mais visível e toca (rolagem suave estilo TikTok)
    private void snapAndPlay() {
        if (feedListView == null) return;
        int first  = feedListView.getFirstVisiblePosition();
        int last   = feedListView.getLastVisiblePosition();
        int center = feedListView.getHeight() / 2;
        int bestPos  = first;
        int bestDist = Integer.MAX_VALUE;

        for (int i = first; i <= last; i++) {
            View child = feedListView.getChildAt(i - first);
            if (child == null) continue;
            int childCenter = (child.getTop() + child.getBottom()) / 2;
            int dist = Math.abs(childCenter - center);
            if (dist < bestDist) {
                bestDist = dist;
                bestPos  = i;
            }
        }
        // Snap suave: move item para topo em 250ms
        feedListView.smoothScrollToPositionFromTop(bestPos, 0, 250);
        playAt(bestPos);
    }

    private void playAt(final int position) {
        if (position == currentPlayingPosition) return;

        stopAllVideos();

        int childIndex = position - feedListView.getFirstVisiblePosition();
        View target = feedListView.getChildAt(childIndex);
        if (target == null) return;

        final VideoView  vv            = (VideoView)  target.findViewById(R.id.vv_episode);
        final View       playContainer = target.findViewById(R.id.fl_play_container);
        final android.widget.ImageView ivThumb = (android.widget.ImageView) target.findViewById(R.id.iv_thumbnail);
        if (vv == null) return;

        if (position < episodes.size()) {
            Episode ep  = episodes.get(position);
            String  url = ep.getVideoUrl();
            if (url != null && !url.isEmpty()) {
                currentPlayingPosition = position;
                vv.setVideoURI(Uri.parse(url));
                vv.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                    public void onPrepared(android.media.MediaPlayer mp) {
                        mp.start();
                        // Esconde thumbnail quando vídeo começa
                        if (ivThumb != null) ivThumb.setVisibility(android.view.View.GONE);
                        if (playContainer != null) playContainer.setVisibility(android.view.View.GONE);
                    }
                });
                vv.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                    public void onCompletion(android.media.MediaPlayer mp) {
                        mp.start(); // loop
                        if (ivThumb != null) ivThumb.setVisibility(android.view.View.GONE);
                        if (playContainer != null) playContainer.setVisibility(android.view.View.GONE);
                    }
                });
                vv.start();
            }
        }
    }

    private void stopAllVideos() {
        if (feedListView == null) return;
        for (int i = 0; i < feedListView.getChildCount(); i++) {
            View child = feedListView.getChildAt(i);
            if (child == null) continue;
            VideoView vv = (VideoView) child.findViewById(R.id.vv_episode);
            if (vv != null && vv.isPlaying()) vv.pause();
            // Mostra thumbnail de volta quando o vídeo para
            android.widget.ImageView ivThumb = (android.widget.ImageView) child.findViewById(R.id.iv_thumbnail);
            if (ivThumb != null) ivThumb.setVisibility(View.VISIBLE);
            View playContainer = child.findViewById(R.id.fl_play_container);
            if (playContainer != null) {
                playContainer.setVisibility(View.VISIBLE);
            }
        }
        currentPlayingPosition = -1;
    }

    private void loadFeed() {
        serverRepository.loadFeed(40, new ServerRepository.FeedCallback() {
            public void onSuccess(final List<Episode> loaded, boolean fromServer) {
                if (getActivity() == null || loaded.isEmpty()) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        episodes.clear();
                        episodes.addAll(loaded);
                        adapter.notifyDataSetChanged();
                        currentPlayingPosition = -1;
                        feedListView.post(new Runnable() {
                            public void run() { playAt(0); }
                        });
                    }
                });
            }
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            feedListView.post(new Runnable() {
                                public void run() { playAt(0); }
                            });
                        }
                    });
                }
            }
        });
    }

    // Toque: toggle pause/play com ícone atualizado
    public void onEpisodeClick(Episode episode) {
        int position = episodes.indexOf(episode);
        if (position < 0) return;

        int childIndex = position - feedListView.getFirstVisiblePosition();
        View child = feedListView.getChildAt(childIndex);
        if (child == null) return;

        VideoView vv           = (VideoView) child.findViewById(R.id.vv_episode);
        View      playContainer = child.findViewById(R.id.fl_play_container);
        if (vv == null) return;

        if (!prefs.isVipActive() && !episode.isFree()) {
            int cost = episode.getCoinCost();
            if (!prefs.spendCoins(cost)) {
                showNoCoinsDialog();
                return;
            }
            prefs.incrementEpisodesWatched();
            updateCoinDisplay();
        }

        episodesSinceLastAd++;
        if (episodesSinceLastAd >= AD_EVERY_N_EPISODES) {
            episodesSinceLastAd = 0;
            pendingEpisode = episode;
            Intent intent = new Intent(getActivity(), AdWebViewActivity.class);
            intent.putExtra(AdWebViewActivity.EXTRA_AD_URL, AD_URL);
            intent.putExtra(AdWebViewActivity.EXTRA_AD_TYPE, "episode");
            startActivityForResult(intent, AdWebViewActivity.REQUEST_AD_EPISODE);
            return;
        }

        if (vv.isPlaying()) {
            vv.pause();
            // Mostra container play quando pausado
            if (playContainer != null) playContainer.setVisibility(View.VISIBLE);
        } else {
            vv.start();
            // Esconde container quando tocando
            if (playContainer != null) playContainer.setVisibility(View.GONE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AdWebViewActivity.REQUEST_AD_EPISODE) {
            // Após o anúncio, continua a reprodução normalmente
            if (pendingEpisode != null) {
                Episode ep = pendingEpisode;
                pendingEpisode = null;
                onEpisodeClick(ep);
            }
        }
    }

    private void showNoCoinsDialog() {
        if (getActivity() == null) return;
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_no_coins);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_bottom_sheet);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        Button btnVip    = (Button) dialog.findViewById(R.id.btn_go_vip);
        Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);
        if (btnVip != null) {
            btnVip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToVip();
                    }
                }
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { dialog.dismiss(); }
            });
        }
        dialog.show();
    }
}
