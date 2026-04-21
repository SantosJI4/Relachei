package com.noveflix.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.noveflix.app.MainActivity;
import com.noveflix.app.R;
import com.noveflix.app.utils.PrefsManager;

public class ProfileFragment extends Fragment {

    private PrefsManager prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PrefsManager.getInstance(requireContext());

        TextView tvCoins      = view.findViewById(R.id.tv_profile_coins);
        TextView tvEpisodes   = view.findViewById(R.id.tv_profile_episodes);
        TextView tvSaved      = view.findViewById(R.id.tv_profile_saved);
        TextView tvVipBadge   = view.findViewById(R.id.tv_vip_badge);
        LinearLayout btnVip   = view.findViewById(R.id.btn_profile_vip);
        LinearLayout btnCoins = view.findViewById(R.id.btn_profile_coins);
        LinearLayout btnHistory = view.findViewById(R.id.btn_profile_history);
        LinearLayout btnSettings = view.findViewById(R.id.btn_profile_settings);

        updateStats(tvCoins, tvEpisodes, tvSaved, tvVipBadge);

        btnVip.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToVip();
            }
        });

        btnCoins.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Em breve: loja de moedas!", Toast.LENGTH_SHORT).show());

        btnHistory.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Em breve: histórico!", Toast.LENGTH_SHORT).show());

        btnSettings.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Em breve: configurações!", Toast.LENGTH_SHORT).show());
    }

    @Override
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
