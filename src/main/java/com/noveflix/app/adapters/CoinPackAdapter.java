package com.noveflix.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.noveflix.app.R;
import com.noveflix.app.models.CoinPack;

import java.util.List;

public class CoinPackAdapter extends BaseAdapter {

    public interface OnCoinPackSelectedListener {
        void onCoinPackSelected(CoinPack pack);
    }

    private final Context                  context;
    private final List<CoinPack>           packs;
    private final OnCoinPackSelectedListener listener;

    public CoinPackAdapter(Context context, List<CoinPack> packs, OnCoinPackSelectedListener listener) {
        this.context  = context;
        this.packs    = packs;
        this.listener = listener;
    }

    public int getCount() { return packs.size(); }
    public Object getItem(int pos) { return packs.get(pos); }
    public long getItemId(int pos) { return pos; }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_coin_pack, parent, false);
        }

        final CoinPack pack = packs.get(position);

        TextView tvName     = (TextView) convertView.findViewById(R.id.tv_coin_pack_name);
        TextView tvCoins    = (TextView) convertView.findViewById(R.id.tv_coin_pack_coins);
        TextView tvBonus    = (TextView) convertView.findViewById(R.id.tv_coin_pack_bonus);
        TextView tvPrice    = (TextView) convertView.findViewById(R.id.tv_coin_pack_price);
        TextView tvSavings  = (TextView) convertView.findViewById(R.id.tv_coin_pack_savings);
        Button   btnBuy     = (Button)   convertView.findViewById(R.id.btn_buy_pack);

        tvName.setText(pack.getName());
        tvCoins.setText("" + pack.getCoins() + " moedas");
        tvPrice.setText(pack.getPrice());

        if (pack.getBonusCoins() > 0) {
            tvBonus.setVisibility(View.VISIBLE);
            tvBonus.setText("+" + pack.getBonusCoins() + " bonus");
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
            convertView.setBackgroundResource(R.drawable.shape_card_vip_popular);
        } else {
            convertView.setBackgroundResource(R.drawable.shape_card_dark);
        }

        btnBuy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onCoinPackSelected(pack);
            }
        });

        return convertView;
    }
}
