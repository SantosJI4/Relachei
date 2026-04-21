package com.noveflix.app.fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.noveflix.app.MainActivity;
import com.noveflix.app.PlayerActivity;
import com.noveflix.app.R;
import com.noveflix.app.adapters.EpisodeListAdapter;
import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.Episode;
import com.noveflix.app.models.Novela;
import com.noveflix.app.network.ServerRepository;
import com.noveflix.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CatalogFragment extends Fragment {

    private LinearLayout    catalogContainer;
    private View            loadingView;
    private PrefsManager    prefs;
    private ServerRepository repository;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs           = PrefsManager.getInstance(getActivity());
        catalogContainer = (LinearLayout) view.findViewById(R.id.catalog_container);
        loadingView     = view.findViewById(R.id.catalog_loading);
        repository      = new ServerRepository();
        loadNovelas();
    }

    private void loadNovelas() {
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);

        repository.loadNovelas(new ServerRepository.NovelasCallback() {
            public void onSuccess(final List<Novela> novelas) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (loadingView != null) loadingView.setVisibility(View.GONE);
                        buildCatalog(novelas);
                    }
                });
            }
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (loadingView != null) loadingView.setVisibility(View.GONE);
                        // Fallback para dados mock se servidor offline
                        buildCatalog(MockDataProvider.getNovelas());
                    }
                });
            }
        });
    }

    private void buildCatalog(List<Novela> novelas) {
        if (catalogContainer == null) return;
        catalogContainer.removeAllViews();

        // Agrupa por país
        Map<String, List<Novela>> byCountry = new LinkedHashMap<>();
        for (Novela n : novelas) {
            String key = n.getCountryFlag() + " " + getCountryName(n.getCountryCode());
            List<Novela> list = byCountry.get(key);
            if (list == null) {
                list = new ArrayList<>();
                byCountry.put(key, list);
            }
            list.add(n);
        }

        // Se só tem uma "categoria" (sem país definido), mostra "Todas as Novelas"
        if (byCountry.size() == 1 && byCountry.containsKey(" ")) {
            byCountry = new LinkedHashMap<>();
            byCountry.put("Todas as Novelas", novelas);
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        for (Map.Entry<String, List<Novela>> entry : byCountry.entrySet()) {
            addCategorySection(inflater, entry.getKey(), entry.getValue());
        }
    }

    private void addCategorySection(LayoutInflater inflater, String categoryName,
                                    List<Novela> novelas) {
        View section = inflater.inflate(R.layout.item_category_section, catalogContainer, false);
        TextView tvCategory     = (TextView)     section.findViewById(R.id.tv_category_name);
        LinearLayout cardsContainer = (LinearLayout) section.findViewById(R.id.cards_container);

        if (tvCategory != null) tvCategory.setText(categoryName);

        for (final Novela novela : novelas) {
            View card        = inflater.inflate(R.layout.item_novela_card, cardsContainer, false);
            ImageView ivThumb  = (ImageView) card.findViewById(R.id.iv_novela_thumb);
            TextView  tvTitle  = (TextView)  card.findViewById(R.id.tv_novela_card_title);
            TextView  tvEpCount = (TextView) card.findViewById(R.id.tv_episode_count);

            if (tvTitle   != null) tvTitle.setText(novela.getTitle());

            // Mostra contagem correta: usa episodeCount do servidor (sem precisar dos episódios)
            int epCount = novela.getEpisodeCount();
            if (tvEpCount != null) tvEpCount.setText(epCount + " ep.");

            // Usa poster_url como capa (campo thumbnailUrl do model)
            String thumb = novela.getThumbnailUrl();
            if (ivThumb != null && thumb != null && !thumb.isEmpty()) {
                Glide.with(getActivity())
                        .load(thumb)
                        .centerCrop()
                        .placeholder(R.color.bg_card)
                        .into(ivThumb);
            }

            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openEpisodesDialog(novela);
                }
            });

            if (cardsContainer != null) cardsContainer.addView(card);
        }

        catalogContainer.addView(section);
    }

    private void openEpisodesDialog(final Novela novela) {
        if (getActivity() == null) return;

        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_episodes);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_bottom_sheet);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final TextView  tvTitle      = (TextView)  dialog.findViewById(R.id.tv_dialog_novela_title);
        final TextView  tvLabel      = (TextView)  dialog.findViewById(R.id.tv_dialog_episodes_label);
        final ListView  listEpisodes = (ListView)  dialog.findViewById(R.id.list_episodes);

        if (tvTitle != null) tvTitle.setText(novela.getTitle());
        if (tvLabel != null) tvLabel.setText("Carregando episódios...");

        dialog.show();

        // Busca episódios reais do servidor
        repository.loadNovelaEpisodes(novela.getId(), new ServerRepository.EpisodesCallback() {
            public void onSuccess(final List<Episode> episodes) {
                if (getActivity() == null || !dialog.isShowing()) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (tvLabel != null)
                            tvLabel.setText(episodes.size() + " episódios");

                        EpisodeListAdapter adapter = new EpisodeListAdapter(
                                getActivity(),
                                episodes,
                                new EpisodeListAdapter.OnPlayListener() {
                                    public void onPlay(Episode episode) {
                                        dialog.dismiss();
                                        playEpisode(episode);
                                    }
                                });
                        if (listEpisodes != null) listEpisodes.setAdapter(adapter);
                    }
                });
            }
            public void onError(String message) {
                if (getActivity() == null || !dialog.isShowing()) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (tvLabel != null) tvLabel.setText("Erro ao carregar episódios");
                    }
                });
            }
        });
    }

    private void playEpisode(Episode episode) {
        if (getActivity() == null) return;

        if (!prefs.isVipActive() && !episode.isFree()) {
            if (!prefs.spendCoins(episode.getCoinCost())) {
                showNoCoinsDialog();
                return;
            }
            prefs.incrementEpisodesWatched();
        }

        String url = episode.getVideoUrl();
        if (url == null || url.isEmpty()) {
            Toast.makeText(getActivity(), "Episódio em breve!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, url);
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_TITLE, episode.getTitle());
        intent.putExtra(PlayerActivity.EXTRA_NOVELA_TITLE, episode.getNovelaTitle());
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_NUM, episode.getEpisodeNumber());
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
                public void onClick(View v) { dialog.dismiss(); }
            });
        }
        dialog.show();
    }

    private String getCountryName(String code) {
        if ("JP".equals(code)) return "Japão";
        if ("KR".equals(code)) return "Coreia";
        if ("TR".equals(code)) return "Turquia";
        if ("MX".equals(code)) return "México";
        if ("CN".equals(code)) return "China";
        if ("BR".equals(code)) return "Brasil";
        return code != null && !code.isEmpty() ? code : "Internacional";
    }
}


public class CatalogFragment extends Fragment {

    private LinearLayout catalogContainer;
    private PrefsManager prefs;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(getActivity());
        catalogContainer = (LinearLayout) view.findViewById(R.id.catalog_container);
        buildCatalog();
    }

    private void buildCatalog() {
        List<Novela> novelas = MockDataProvider.getNovelas();

        // Agrupa por país mantendo a ordem de inserção
        Map<String, List<Novela>> byCountry = new LinkedHashMap<>();
        for (Novela n : novelas) {
            String key = n.getCountryFlag() + " " + getCountryName(n.getCountryCode());
            List<Novela> list = byCountry.get(key);
            if (list == null) {
                list = new ArrayList<>();
                byCountry.put(key, list);
            }
            list.add(n);
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        for (Map.Entry<String, List<Novela>> entry : byCountry.entrySet()) {
            addCategorySection(inflater, entry.getKey(), entry.getValue());
        }
    }

    private void addCategorySection(LayoutInflater inflater, String categoryName,
                                    List<Novela> novelas) {
        View section = inflater.inflate(R.layout.item_category_section, catalogContainer, false);
        TextView tvCategory = (TextView) section.findViewById(R.id.tv_category_name);
        LinearLayout cardsContainer = (LinearLayout) section.findViewById(R.id.cards_container);

        if (tvCategory != null) tvCategory.setText(categoryName);

        for (final Novela novela : novelas) {
            View card = inflater.inflate(R.layout.item_novela_card, cardsContainer, false);
            ImageView ivThumb  = (ImageView) card.findViewById(R.id.iv_novela_thumb);
            TextView  tvTitle  = (TextView)  card.findViewById(R.id.tv_novela_card_title);
            TextView  tvEpCount = (TextView) card.findViewById(R.id.tv_episode_count);

            if (tvTitle   != null) tvTitle.setText(novela.getTitle());
            if (tvEpCount != null) tvEpCount.setText(novela.getEpisodeCount() + " ep.");

            if (ivThumb != null && novela.getThumbnailUrl() != null
                    && !novela.getThumbnailUrl().isEmpty()) {
                Glide.with(getActivity())
                        .load(novela.getThumbnailUrl())
                        .centerCrop()
                        .into(ivThumb);
            }

            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showEpisodesDialog(novela);
                }
            });

            if (cardsContainer != null) cardsContainer.addView(card);
        }

        catalogContainer.addView(section);
    }

    private void showEpisodesDialog(final Novela novela) {
        if (getActivity() == null) return;

        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_episodes);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_bottom_sheet);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle      = (TextView) dialog.findViewById(R.id.tv_dialog_novela_title);
        ListView listEpisodes = (ListView) dialog.findViewById(R.id.list_episodes);

        if (tvTitle != null) tvTitle.setText(novela.getTitle());

        EpisodeListAdapter adapter = new EpisodeListAdapter(
                getActivity(),
                novela.getEpisodes(),
                new EpisodeListAdapter.OnPlayListener() {
                    public void onPlay(Episode episode) {
                        dialog.dismiss();
                        playEpisode(episode);
                    }
                });

        if (listEpisodes != null) listEpisodes.setAdapter(adapter);

        dialog.show();
    }

    private void playEpisode(Episode episode) {
        if (getActivity() == null) return;

        if (!prefs.isVipActive() && !episode.isFree()) {
            if (!prefs.spendCoins(episode.getCoinCost())) {
                showNoCoinsDialog();
                return;
            }
            prefs.incrementEpisodesWatched();
        }

        String url = episode.getVideoUrl();
        if (url == null || url.isEmpty()) {
            Toast.makeText(getActivity(), "Episódio em breve!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, url);
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_TITLE, episode.getTitle());
        intent.putExtra(PlayerActivity.EXTRA_NOVELA_TITLE, episode.getNovelaTitle());
        intent.putExtra(PlayerActivity.EXTRA_EPISODE_NUM, episode.getEpisodeNumber());
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
                public void onClick(View v) { dialog.dismiss(); }
            });
        }
        dialog.show();
    }

    private String getCountryName(String code) {
        if ("JP".equals(code)) return "Japão";
        if ("KR".equals(code)) return "Coreia";
        if ("TR".equals(code)) return "Turquia";
        if ("MX".equals(code)) return "México";
        if ("CN".equals(code)) return "China";
        return code;
    }
}
