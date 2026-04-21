package com.noveflix.app.fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;

import com.noveflix.app.MainActivity;
import com.noveflix.app.R;
import com.noveflix.app.adapters.FeedAdapter;
import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.Episode;
import com.noveflix.app.network.ServerRepository;
import com.noveflix.app.utils.PrefsManager;

import java.util.List;

public class HomeFragment extends Fragment implements FeedAdapter.OnEpisodeClickListener {

    private ListView         feedListView;
    private FeedAdapter      adapter;
    private TextView         tvCoinBalance;
    private PrefsManager     prefs;
    private List<Episode>    episodes;
    private ServerRepository serverRepository;
    private int              currentPlayingPosition = -1;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs         = PrefsManager.getInstance(getActivity());
        tvCoinBalance = (TextView) view.findViewById(R.id.tv_coin_balance_home);
        feedListView  = (ListView) view.findViewById(R.id.feed_list_view);

        serverRepository = new ServerRepository();
        episodes = MockDataProvider.getFeedEpisodes();
        adapter  = new FeedAdapter(getActivity(), episodes, this);
        feedListView.setAdapter(adapter);

        // === TIKTOK SCROLL LISTENER ===
        feedListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    playMostVisible();
                }
            }
            public void onScroll(AbsListView view, int firstVisible,
                                 int visibleCount, int totalCount) {}
        });

        updateCoinDisplay();
        loadFeed();
    }

    public void onResume() {
        super.onResume();
        updateCoinDisplay();
        // Auto-play ao voltar
        feedListView.post(new Runnable() {
            public void run() { playMostVisible(); }
        });
    }

    public void onPause() {
        super.onPause();
        stopAllVideos();
    }

    private void updateCoinDisplay() {
        if (tvCoinBalance != null) {
            tvCoinBalance.setText("" + prefs.getCoins() + " moedas");
        }
    }

    // Encontra o item mais centralizado na tela e auto-play
    private void playMostVisible() {
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
        playAt(bestPos);
    }

    private void playAt(int position) {
        if (position == currentPlayingPosition) return;

        // Para todos os vídeos visíveis
        stopAllVideos();

        // Toca o item na posição alvo
        int childIndex = position - feedListView.getFirstVisiblePosition();
        View target = feedListView.getChildAt(childIndex);
        if (target == null) return;

        VideoView vv = (VideoView) target.findViewById(R.id.vv_episode);
        if (vv == null) return;

        if (position < episodes.size()) {
            Episode ep = episodes.get(position);
            String url = ep.getVideoUrl();
            if (url != null && !url.isEmpty()) {
                currentPlayingPosition = position;
                vv.setVideoURI(Uri.parse(url));
                vv.start();
                // Looping
                vv.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                    public void onCompletion(android.media.MediaPlayer mp) {
                        mp.start(); // loop
                    }
                });
            }
        }
    }

    private void stopAllVideos() {
        if (feedListView == null) return;
        for (int i = 0; i < feedListView.getChildCount(); i++) {
            View child = feedListView.getChildAt(i);
            if (child == null) continue;
            VideoView vv = (VideoView) child.findViewById(R.id.vv_episode);
            if (vv != null && vv.isPlaying()) {
                vv.pause();
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
                        // Auto-play primeiro item após carregar
                        feedListView.post(new Runnable() {
                            public void run() { playAt(0); }
                        });
                    }
                });
            }
            public void onError(String message) {
                // feed mock já exibido
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

    // FeedAdapter.OnEpisodeClickListener - toque pausa/retoma
    public void onEpisodeClick(Episode episode) {
        int position = episodes.indexOf(episode);
        if (position < 0) return;

        int childIndex = position - feedListView.getFirstVisiblePosition();
        View child = feedListView.getChildAt(childIndex);
        if (child == null) return;

        VideoView vv = (VideoView) child.findViewById(R.id.vv_episode);
        if (vv == null) return;

        if (!prefs.isVipActive() && !episode.isFree()) {
            if (!prefs.spendCoins(2)) {
                showNoCoinsDialog();
                return;
            }
            prefs.incrementEpisodesWatched();
            updateCoinDisplay();
        }

        // Toggle pause/play ao tocar
        if (vv.isPlaying()) {
            vv.pause();
        } else {
            vv.start();
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

public class HomeFragment extends Fragment implements FeedAdapter.OnEpisodeClickListener {

    private ListView         feedListView;
    private FeedAdapter      adapter;
    private TextView         tvCoinBalance;
    private PrefsManager     prefs;
    private List<Episode>    episodes;
    private ServerRepository serverRepository;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs         = PrefsManager.getInstance(getActivity());
        tvCoinBalance = (TextView)  view.findViewById(R.id.tv_coin_balance_home);
        feedListView  = (ListView)  view.findViewById(R.id.feed_list_view);

        serverRepository = new ServerRepository();
        episodes = MockDataProvider.getFeedEpisodes();
        adapter  = new FeedAdapter(getActivity(), episodes, this);
        feedListView.setAdapter(adapter);

        updateCoinDisplay();
        loadFeed();
    }

    public void onResume() {
        super.onResume();
        updateCoinDisplay();
    }

    private void updateCoinDisplay() {
        if (tvCoinBalance != null) {
            tvCoinBalance.setText("" + prefs.getCoins() + " moedas");
        }
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
                    }
                });
            }

            public void onError(String message) {
                // feed mock ja exibido
            }
        });
    }

    // FeedAdapter.OnEpisodeClickListener
    public void onEpisodeClick(Episode episode) {
        if (prefs.isVipActive() || episode.isFree()) {
            openPlayer(episode);
            prefs.incrementEpisodesWatched();
        } else {
            if (prefs.spendCoins(2)) {
                prefs.incrementEpisodesWatched();
                updateCoinDisplay();
                openPlayer(episode);
            } else {
                showNoCoinsDialog();
            }
        }
    }

    private void openPlayer(Episode episode) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL,     episode.getVideoUrl());
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_TITLE, episode.getTitle());
        intent.putExtra(PlayerActivity.EXTRA_NOVELA_TITLE,  episode.getNovelaTitle());
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_NUM,   episode.getEpisodeNumber());
        startActivity(intent);
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
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }
}
