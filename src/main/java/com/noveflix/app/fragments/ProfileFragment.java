package com.noveflix.app.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

        TextView     tvCoins    = (TextView)     view.findViewById(R.id.tv_profile_coins);
        TextView     tvEpisodes = (TextView)     view.findViewById(R.id.tv_profile_episodes);
        TextView     tvSaved    = (TextView)     view.findViewById(R.id.tv_profile_saved);
        TextView     tvVipBadge = (TextView)     view.findViewById(R.id.tv_vip_badge);
        LinearLayout btnVip     = (LinearLayout) view.findViewById(R.id.btn_profile_vip);
        LinearLayout btnCoins   = (LinearLayout) view.findViewById(R.id.btn_profile_coins);
        LinearLayout btnHistory = (LinearLayout) view.findViewById(R.id.btn_profile_history);
        LinearLayout btnSettings= (LinearLayout) view.findViewById(R.id.btn_profile_settings);

        updateStats(tvCoins, tvEpisodes, tvSaved, tvVipBadge);

        if (btnVip != null) {
            btnVip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToVip();
                    }
                }
            });
        }

        if (btnCoins != null) {
            btnCoins.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Em breve: loja de moedas!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Em breve: historico!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Em breve: configuracoes!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void onResume() {
        super.onResume();
        View view = getView();
        if (view == null) return;
        updateStats(
                (TextView) view.findViewById(R.id.tv_profile_coins),
                (TextView) view.findViewById(R.id.tv_profile_episodes),
                (TextView) view.findViewById(R.id.tv_profile_saved),
                (TextView) view.findViewById(R.id.tv_vip_badge)
        );
    }

    private void updateStats(TextView tvCoins, TextView tvEpisodes,
                             TextView tvSaved, TextView tvVipBadge) {
        if (tvCoins    != null) tvCoins.setText(prefs.getCoins() + " moedas");
        if (tvEpisodes != null) tvEpisodes.setText(String.valueOf(prefs.getEpisodesWatched()));
        if (tvSaved    != null) tvSaved.setText(String.valueOf(prefs.getNovelasSaved()));
        if (tvVipBadge != null) {
            if (prefs.isVipActive()) {
                tvVipBadge.setVisibility(View.VISIBLE);
                tvVipBadge.setText("VIP");
            } else {
                tvVipBadge.setVisibility(View.GONE);
            }
        }
    }
}
