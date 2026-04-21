package com.noveflix.app.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import com.noveflix.app.R;
import com.noveflix.app.data.MockDataProvider;
import com.noveflix.app.models.CoinPack;
import com.noveflix.app.models.VipPlan;
import com.noveflix.app.utils.PrefsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VipFragment extends Fragment {

    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7049530106653071/8124737911";
    private static final int    REWARD_SHORT  = 1;
    private static final int    REWARD_LONG   = 3;

    private InterstitialAd mInterstitialAd = null;

    private PrefsManager prefs;
    private TextView     tvVipStatus;
    private TextView     tvVipCoins;
    private TextView     tvVipCoinsActive;
    private View         layoutVipActive;
    private View         layoutVipBenefits;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vip, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs             = PrefsManager.getInstance(getActivity());
        tvVipStatus       = (TextView) view.findViewById(R.id.tv_vip_status);
        tvVipCoins        = (TextView) view.findViewById(R.id.tv_vip_coins);
        tvVipCoinsActive  = (TextView) view.findViewById(R.id.tv_vip_coins_active);
        layoutVipActive   = view.findViewById(R.id.layout_vip_active);
        layoutVipBenefits = view.findViewById(R.id.layout_vip_benefits);

        inflatePlans(view);
        inflateCoinPacks(view);
        updateVipStatus();
        wireAdButtons(view);
        preloadInterstitial();
    }

    private void preloadInterstitial() {
        if (getActivity() == null) return;
        InterstitialAd.load(getActivity(), INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    public void onAdLoaded(InterstitialAd ad) {
                        mInterstitialAd = ad;
                    }
                    public void onAdFailedToLoad(LoadAdError error) {
                        mInterstitialAd = null;
                    }
                });
    }

    private void wireAdButtons(View root) {
        Button btnShort = (Button) root.findViewById(R.id.btn_ad_short);
        Button btnLong  = (Button) root.findViewById(R.id.btn_ad_long);

        if (btnShort != null) {
            btnShort.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showInterstitialForCoins(REWARD_SHORT);
                }
            });
        }
        if (btnLong != null) {
            btnLong.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showInterstitialForCoins(REWARD_LONG);
                }
            });
        }
    }

    private void showInterstitialForCoins(final int reward) {
        if (mInterstitialAd != null && getActivity() != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    preloadInterstitial();
                    prefs.addCoins(reward);
                    updateVipStatus();
                    Toast.makeText(getActivity(),
                            "+" + reward + " moedas adicionadas! \uD83E\uDE99",
                            Toast.LENGTH_SHORT).show();
                }
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mInterstitialAd = null;
                    preloadInterstitial();
                }
            });
            mInterstitialAd.show(getActivity());
        } else {
            Toast.makeText(getActivity(),
                    "Anúncio não disponível, tente novamente em instantes.",
                    Toast.LENGTH_SHORT).show();
            preloadInterstitial();
        }
    }

    private void openAdPage() {} // mantido para compatibilidade

    private void inflatePlans(View root) {
        LinearLayout container = (LinearLayout) root.findViewById(R.id.ll_vip_plans);
        if (container == null) return;

        List<VipPlan> plans = MockDataProvider.getVipPlans();
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        for (int i = 0; i < plans.size(); i++) {
            final VipPlan plan = plans.get(i);
            View item = inflater.inflate(R.layout.item_vip_plan, container, false);

            TextView tvName  = (TextView) item.findViewById(R.id.tv_plan_name);
            TextView tvPrice = (TextView) item.findViewById(R.id.tv_plan_price);
            TextView tvSub   = (TextView) item.findViewById(R.id.tv_plan_subtitle);
            Button   btnSub  = (Button)   item.findViewById(R.id.btn_subscribe);

            tvName.setText(plan.getName());
            tvPrice.setText(plan.getPrice());
            tvSub.setText(plan.getSubtitle());

            btnSub.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onPlanSelected(plan);
                }
            });

            container.addView(item);
        }
    }

    private void inflateCoinPacks(View root) {
        LinearLayout container = (LinearLayout) root.findViewById(R.id.ll_coin_packs);
        if (container == null) return;

        List<CoinPack> packs = MockDataProvider.getCoinPacks();
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        for (int i = 0; i < packs.size(); i++) {
            final CoinPack pack = packs.get(i);
            View item = inflater.inflate(R.layout.item_coin_pack, container, false);

            TextView tvName  = (TextView) item.findViewById(R.id.tv_coin_pack_name);
            TextView tvCoins = (TextView) item.findViewById(R.id.tv_coin_pack_coins);
            TextView tvPrice = (TextView) item.findViewById(R.id.tv_coin_pack_price);
            Button   btnBuy  = (Button)   item.findViewById(R.id.btn_buy_pack);

            tvName.setText(pack.getName());
            tvCoins.setText("" + pack.getTotalCoins() + " moedas");
            tvPrice.setText(pack.getPrice());

            btnBuy.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onCoinPackSelected(pack);
                }
            });

            container.addView(item);
        }
    }

    public void onResume() {
        super.onResume();
        updateVipStatus();
    }

    private void updateVipStatus() {
        if (prefs.isVipActive()) {
            layoutVipActive.setVisibility(View.VISIBLE);
            layoutVipBenefits.setVisibility(View.GONE);
            tvVipCoins.setText("" + prefs.getCoins() + " moedas");
            if (tvVipCoinsActive != null) {
                tvVipCoinsActive.setText("" + prefs.getCoins() + " moedas");
            }
            long expiry = prefs.getVipExpiry();
            if (expiry > 0) {
                String exp = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                        Locale.getDefault()).format(new Date(expiry));
                tvVipStatus.setText("VIP ativo - expira em " + exp);
            } else {
                int rem = prefs.getRemainingVipNovelas();
                tvVipStatus.setText("Pack VIP - " + rem + " novela(s)");
            }
        } else {
            layoutVipActive.setVisibility(View.GONE);
            layoutVipBenefits.setVisibility(View.VISIBLE);
            tvVipCoins.setText("" + prefs.getCoins() + " moedas");
        }
    }

    private void onPlanSelected(VipPlan plan) {
        prefs.activateVip(plan.getType(), plan.getDurationMillis(), plan.getNovelaPacks());
        if (plan.getBonusCoins() > 0) {
            prefs.addCoins(plan.getBonusCoins());
        }
        updateVipStatus();
        Toast.makeText(getActivity(), plan.getName() + " ativado!", Toast.LENGTH_LONG).show();
    }

    private void onCoinPackSelected(CoinPack pack) {
        prefs.addCoins(pack.getTotalCoins());
        updateVipStatus();
        Toast.makeText(getActivity(), "+" + pack.getTotalCoins() + " moedas!", Toast.LENGTH_SHORT).show();
    }
}
