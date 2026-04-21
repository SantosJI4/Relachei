package com.noveflix.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.noveflix.app.R;
import com.noveflix.app.adapters.CoinPackAdapter;
import com.noveflix.app.adapters.VipPlanAdapter;
import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.CoinPack;
import com.noveflix.app.models.VipPlan;
import com.noveflix.app.utils.PrefsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VipFragment extends Fragment {

    private PrefsManager prefs;
    private TextView tvVipStatus;
    private TextView tvVipCoins;
    private TextView tvVipCoinsActive;
    private View     layoutVipActive;
    private View     layoutVipBenefits;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PrefsManager.getInstance(requireContext());

        tvVipStatus      = view.findViewById(R.id.tv_vip_status);
        // tv_vip_coins é o saldo na aba benefits; tv_vip_coins_active é na aba ativo
        tvVipCoins       = view.findViewById(R.id.tv_vip_coins);
        tvVipCoinsActive = view.findViewById(R.id.tv_vip_coins_active);
        layoutVipActive  = view.findViewById(R.id.layout_vip_active);
        layoutVipBenefits = view.findViewById(R.id.layout_vip_benefits);

        // Setup VIP plans RecyclerView (rolagem horizontal)
        RecyclerView rvPlans = view.findViewById(R.id.rv_vip_plans);
        List<VipPlan> plans  = MockDataProvider.getVipPlans();
        VipPlanAdapter planAdapter = new VipPlanAdapter(plans, new VipPlanAdapter.OnPlanSelectedListener() {
            @Override
            public void onPlanSelected(VipPlan plan) {
                onPlanSelectedInternal(plan);
            }
        });
        rvPlans.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPlans.setAdapter(planAdapter);

        // Setup Coin packs RecyclerView
        RecyclerView rvCoins = view.findViewById(R.id.rv_coin_packs);
        List<CoinPack> packs = MockDataProvider.getCoinPacks();
        CoinPackAdapter coinAdapter = new CoinPackAdapter(packs, new CoinPackAdapter.OnCoinPackSelectedListener() {
            @Override
            public void onCoinPackSelected(CoinPack pack) {
                onCoinPackSelectedInternal(pack);
            }
        });
        rvCoins.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCoins.setAdapter(coinAdapter);

        updateVipStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateVipStatus();
    }

    private void updateVipStatus() {
        if (prefs.isVipActive()) {
            layoutVipActive.setVisibility(View.VISIBLE);
            layoutVipBenefits.setVisibility(View.GONE);
            tvVipCoins.setText("🪙 " + prefs.getCoins() + " moedas");
            if (tvVipCoinsActive != null) tvVipCoinsActive.setText("🪙 " + prefs.getCoins() + " moedas");

            long expiry = prefs.getVipExpiry();
            if (expiry > 0) {
                String expiryStr = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                        Locale.getDefault()).format(new Date(expiry));
                tvVipStatus.setText("✅ VIP ativo — expira em " + expiryStr);
            } else {
                int remaining = prefs.getRemainingVipNovelas();
                tvVipStatus.setText("✅ Pack VIP — " + remaining + " novela(s) restante(s)");
            }
        } else {
            layoutVipActive.setVisibility(View.GONE);
            layoutVipBenefits.setVisibility(View.VISIBLE);
            tvVipCoins.setText("🪙 " + prefs.getCoins() + " moedas");
        }
    }

    private void onPlanSelectedInternal(VipPlan plan) {
        // TODO: integrar Google Play Billing para pagamento real
        prefs.activateVip(plan.getType(), plan.getDurationMillis(), plan.getNovelaPacks());
        if (plan.getBonusCoins() > 0) {
            prefs.addCoins(plan.getBonusCoins());
        }
        updateVipStatus();
        Toast.makeText(requireContext(),
                "✅ " + plan.getName() + " ativado! " +
                        (plan.getBonusCoins() > 0 ? "+" + plan.getBonusCoins() + " moedas bônus!" : ""),
                Toast.LENGTH_LONG).show();
    }

    private void onCoinPackSelectedInternal(CoinPack pack) {
        // TODO: integrar Google Play Billing
        prefs.addCoins(pack.getTotalCoins());
        updateVipStatus();
        Toast.makeText(requireContext(),
                "🪙 +" + pack.getTotalCoins() + " moedas adicionadas!",
                Toast.LENGTH_SHORT).show();
    }
}
