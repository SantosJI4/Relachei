package com.noveflix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.noveflix.app.R;
import com.noveflix.app.models.CoinPack;

import java.util.List;

public class CoinPackAdapter extends RecyclerView.Adapter<CoinPackAdapter.CoinPackViewHolder> {

    public interface OnCoinPackSelectedListener {
        void onCoinPackSelected(CoinPack pack);
    }

    private final List<CoinPack>              packs;
    private final OnCoinPackSelectedListener  listener;

    public CoinPackAdapter(List<CoinPack> packs, OnCoinPackSelectedListener listener) {
        this.packs    = packs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CoinPackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coin_pack, parent, false);
        return new CoinPackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoinPackViewHolder holder, int position) {
        holder.bind(packs.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return packs.size();
    }

    static class CoinPackViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvCoins;
        private final TextView tvBonus;
        private final TextView tvPrice;
        private final TextView tvSavings;
        private final Button   btnBuy;

        CoinPackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tv_coin_pack_name);
            tvCoins   = itemView.findViewById(R.id.tv_coin_pack_coins);
            tvBonus   = itemView.findViewById(R.id.tv_coin_pack_bonus);
            tvPrice   = itemView.findViewById(R.id.tv_coin_pack_price);
            tvSavings = itemView.findViewById(R.id.tv_coin_pack_savings);
            btnBuy    = itemView.findViewById(R.id.btn_buy_pack);
        }

        void bind(CoinPack pack, OnCoinPackSelectedListener listener) {
            tvName.setText(pack.getName());
            tvCoins.setText("🪙 " + pack.getCoins());
            tvPrice.setText(pack.getPrice());

            if (pack.getBonusCoins() > 0) {
                tvBonus.setVisibility(View.VISIBLE);
                tvBonus.setText("+" + pack.getBonusCoins() + " bônus");
            } else {
                tvBonus.setVisibility(View.GONE);
            }

            if (pack.getSavingsPercent() > 0) {
                tvSavings.setVisibility(View.VISIBLE);
                tvSavings.setText("-" + pack.getSavingsPercent() + "%");
            } else {
                tvSavings.setVisibility(View.GONE);
            }

            if (pack.isPopular()) {
                itemView.setBackgroundResource(R.drawable.shape_card_vip_popular);
            } else {
                itemView.setBackgroundResource(R.drawable.shape_card_dark);
            }

            btnBuy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onCoinPackSelected(pack);
                }
            });
        }
    }
}
