package com.noveflix.app.fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.noveflix.app.MainActivity;
import com.noveflix.app.PlayerActivity;
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
