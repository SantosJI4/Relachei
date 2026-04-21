package com.noveflix.app.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.noveflix.app.MainActivity;
import com.noveflix.app.PlayerActivity;
import com.noveflix.app.R;
import com.noveflix.app.adapters.FeedAdapter;
import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.Episode;
import com.noveflix.app.network.ServerRepository;
import com.noveflix.app.utils.PrefsManager;

import java.util.List;

public class HomeFragment extends Fragment implements FeedAdapter.OnEpisodeActionListener {

    private RecyclerView      feedRecyclerView;
    private FeedAdapter     adapter;
    private TextView        tvCoinBalance;
    private PrefsManager    prefs;
    private List<Episode>   episodes;
    private ServerRepository serverRepository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs            = PrefsManager.getInstance(getActivity());
        tvCoinBalance    = view.findViewById(R.id.tv_coin_balance_home);
        feedRecyclerView = (RecyclerView) view.findViewById(R.id.feed_view_pager);
        serverRepository = new ServerRepository();

        // Carrega dados mock enquanto busca dados reais
        episodes = MockDataProvider.getFeedEpisodes();
        adapter  = new FeedAdapter(episodes, this);

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        feedRecyclerView.setLayoutManager(lm);
        feedRecyclerView.setAdapter(adapter);

        updateCoinDisplay();
        loadTmdbFeed();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCoinDisplay();
    }

    private void updateCoinDisplay() {
        if (tvCoinBalance != null) {
            tvCoinBalance.setText("🪙 " + prefs.getCoins());
        }
    }

    /**
     * Busca episódios do servidor NoveFlix (fallback automático para TMDB).
     * Mostra dados mock enquanto carrega.
     */
    private void loadTmdbFeed() {
        serverRepository.loadFeed(40, new ServerRepository.FeedCallback() {
            @Override
            public void onSuccess(final List<Episode> loaded, boolean fromServer) {
                if (getActivity() == null || loaded.isEmpty()) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        episodes.clear();
                        episodes.addAll(loaded);
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String message) {
                // Feed mock já está sendo exibido — silencioso
            }
        });
    }

    // ===================== FeedAdapter.OnEpisodeActionListener =====================

    @Override
    public void onPlayEpisode(Episode episode) {
        if (prefs.isVipActive() || episode.isFree()) {
            openPlayer(episode);
            prefs.incrementEpisodesWatched();
        } else {
            if (prefs.spendCoins(2)) {
                prefs.incrementEpisodesWatched();
                updateCoinDisplay();
                openPlayer(episode);
            } else {
                showNoCoinsDialog(episode);
            }
        }
    }

    @Override
    public void onLikeEpisode(Episode episode, boolean liked) {
        episode.setLiked(liked);
        episode.setLikeCount(episode.getLikeCount() + (liked ? 1 : -1));
    }

    @Override
    public void onShareEpisode(Episode episode) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,
                "Assistindo \"" + episode.getNovelaTitle() + "\" no NoveFlix! 📺");
        startActivity(Intent.createChooser(share, "Compartilhar via"));
    }

    private void openPlayer(Episode episode) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL,     episode.getVideoUrl());
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_TITLE, episode.getTitle());
        intent.putExtra(PlayerActivity.EXTRA_NOVELA_TITLE,  episode.getNovelaTitle());
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_NUM,   episode.getEpisodeNumber());
        startActivity(intent);
    }

    private void showNoCoinsDialog(Episode episode) {
        if (getActivity() == null) return;

        final Dialog dialog = new Dialog(getActivity(), R.style.Theme_NoveFlix);
        dialog.setContentView(R.layout.dialog_no_coins);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_bottom_sheet);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnAd    = dialog.findViewById(R.id.btn_watch_ad);
        Button btnCoins = dialog.findViewById(R.id.btn_buy_coins);
        Button btnVip   = dialog.findViewById(R.id.btn_go_vip);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        btnAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showAdRewardDialog();
            }
        });

        btnCoins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(getActivity(), "Em breve: loja de moedas!", Toast.LENGTH_SHORT).show();
            }
        });

        btnVip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToVip();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showAdRewardDialog() {
        if (getActivity() == null) return;

        final Dialog dialog = new Dialog(getActivity(), R.style.Theme_NoveFlix);
        dialog.setContentView(R.layout.dialog_ad_reward);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_bottom_sheet);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnShortAd = dialog.findViewById(R.id.btn_short_ad);
        View btnLongAd  = dialog.findViewById(R.id.btn_long_ad);
        Button btnCancel  = (Button) dialog.findViewById(R.id.btn_ad_cancel);

        btnShortAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                simulateAdReward(1, "Anúncio rápido assistido! +1 moeda 🪙");
            }
        });

        btnLongAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                simulateAdReward(3, "Anúncio completo assistido! +3 moedas 🪙");
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void simulateAdReward(int coins, String message) {
        prefs.addCoins(coins);
        updateCoinDisplay();
        adapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}
