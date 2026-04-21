package com.noveflix.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.Fragment;

import com.noveflix.app.MainActivity;
import com.noveflix.app.R;
import com.noveflix.app.utils.PrefsManager;

public class ProfileFragment extends Fragment {

    private PrefsManager prefs;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PrefsManager.getInstance(getActivity());

        TextView tvCoins      = view.findViewById(R.id.tv_profile_coins);
        TextView tvEpisodes   = view.findViewById(R.id.tv_profile_episodes);
        TextView tvSaved      = view.findViewById(R.id.tv_profile_saved);
        TextView tvVipBadge   = view.findViewById(R.id.tv_vip_badge);
        LinearLayout btnVip   = view.findViewById(R.id.btn_profile_vip);
        LinearLayout btnCoins = view.findViewById(R.id.btn_profile_coins);
        LinearLayout btnHistory = view.findViewById(R.id.btn_profile_history);
        LinearLayout btnSettings = view.findViewById(R.id.btn_profile_settings);

        updateStats(tvCoins, tvEpisodes, tvSaved, tvVipBadge);

        btnVip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToVip();
                }
            }
        });

        btnCoins.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Em breve: loja de moedas!", Toast.LENGTH_SHORT).show();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Em breve: histórico!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Em breve: configurações!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onResume() {
        super.onResume();
        View view = getView();
        if (view == null) return;
        updateStats(
                view.findViewById(R.id.tv_profile_coins),
                view.findViewById(R.id.tv_profile_episodes),
                view.findViewById(R.id.tv_profile_saved),
                view.findViewById(R.id.tv_vip_badge)
        );
    }

    private void updateStats(TextView tvCoins, TextView tvEpisodes,
                             TextView tvSaved, TextView tvVipBadge) {
        tvCoins.setText(prefs.getCoins() + " moedas");
        tvEpisodes.setText(String.valueOf(prefs.getEpisodesWatched()));
        tvSaved.setText(String.valueOf(prefs.getNovelasSaved()));

        if (prefs.isVipActive()) {
            tvVipBadge.setVisibility(View.VISIBLE);
            tvVipBadge.setText("👑 VIP");
        } else {
            tvVipBadge.setVisibility(View.GONE);
        }
    }
}
