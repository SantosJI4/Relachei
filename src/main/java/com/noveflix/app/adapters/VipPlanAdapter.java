package com.noveflix.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.noveflix.app.R;
import com.noveflix.app.models.VipPlan;

import java.util.List;

public class VipPlanAdapter extends BaseAdapter {

    public interface OnPlanSelectedListener {
        void onPlanSelected(VipPlan plan);
    }

    private final Context               context;
    private final List<VipPlan>         plans;
    private final OnPlanSelectedListener listener;

    public VipPlanAdapter(Context context, List<VipPlan> plans, OnPlanSelectedListener listener) {
        this.context  = context;
        this.plans    = plans;
        this.listener = listener;
    }

    public int getCount() { return plans.size(); }
    public Object getItem(int pos) { return plans.get(pos); }
    public long getItemId(int pos) { return pos; }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_vip_plan, parent, false);
        }

        final VipPlan plan = plans.get(position);

        TextView   tvName        = (TextView)   convertView.findViewById(R.id.tv_plan_name);
        TextView   tvSubtitle    = (TextView)   convertView.findViewById(R.id.tv_plan_subtitle);
        TextView   tvPrice       = (TextView)   convertView.findViewById(R.id.tv_plan_price);
        TextView   tvPriceDetail = (TextView)   convertView.findViewById(R.id.tv_plan_price_detail);
        TextView   tvBadge       = (TextView)   convertView.findViewById(R.id.tv_plan_badge);
        TextView   tvBonusCoins  = (TextView)   convertView.findViewById(R.id.tv_plan_bonus_coins);
        Button     btnSubscribe  = (Button)     convertView.findViewById(R.id.btn_subscribe);

        tvName.setText(plan.getName());
        tvSubtitle.setText(plan.getSubtitle());
        tvPrice.setText(plan.getPrice());
        tvPriceDetail.setText(plan.getPriceDetail());

        if (plan.isPopular()) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText("MAIS POPULAR");
            tvBadge.setBackgroundResource(R.drawable.shape_badge_red);
            convertView.setBackgroundResource(R.drawable.shape_card_vip_popular);
        } else if (plan.isBestValue()) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText("MELHOR PRECO");
            tvBadge.setBackgroundResource(R.drawable.shape_badge_gold);
            convertView.setBackgroundResource(R.drawable.shape_card_vip_gold);
        } else {
            tvBadge.setVisibility(View.GONE);
            convertView.setBackgroundResource(R.drawable.shape_card_dark);
        }

        if (plan.getBonusCoins() > 0) {
            tvBonusCoins.setVisibility(View.VISIBLE);
            tvBonusCoins.setText("+" + plan.getBonusCoins() + " moedas bonus");
        } else {
            tvBonusCoins.setVisibility(View.GONE);
        }

        btnSubscribe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onPlanSelected(plan);
            }
        });

        return convertView;
    }
}
package com.noveflix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.support.v7.widget.RecyclerView;

import com.noveflix.app.R;
import com.noveflix.app.models.VipPlan;

import java.util.List;

public class VipPlanAdapter extends RecyclerView.Adapter<VipPlanAdapter.VipPlanViewHolder> {

    public interface OnPlanSelectedListener {
        void onPlanSelected(VipPlan plan);
    }

    private final List<VipPlan>         plans;
    private final OnPlanSelectedListener listener;

    public VipPlanAdapter(List<VipPlan> plans, OnPlanSelectedListener listener) {
        this.plans    = plans;
        this.listener = listener;
    }

    public VipPlanViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vip_plan, parent, false);
        return new VipPlanViewHolder(view);
    }

    public void onBindViewHolder(VipPlanViewHolder holder, int position) {
        holder.bind(plans.get(position), listener);
    }

    public int getItemCount() {
        return plans.size();
    }

    static class VipPlanViewHolder extends RecyclerView.ViewHolder {
        private final TextView     tvName;
        private final TextView     tvSubtitle;
        private final TextView     tvPrice;
        private final TextView     tvPriceDetail;
        private final TextView     tvBadge;
        private final TextView     tvBonusCoins;
        private final LinearLayout llBenefits;
        private final Button       btnSubscribe;

        VipPlanViewHolder(View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tv_plan_name);
            tvSubtitle   = itemView.findViewById(R.id.tv_plan_subtitle);
            tvPrice      = itemView.findViewById(R.id.tv_plan_price);
            tvPriceDetail = itemView.findViewById(R.id.tv_plan_price_detail);
            tvBadge      = itemView.findViewById(R.id.tv_plan_badge);
            tvBonusCoins = itemView.findViewById(R.id.tv_plan_bonus_coins);
            llBenefits   = itemView.findViewById(R.id.ll_plan_benefits);
            btnSubscribe = itemView.findViewById(R.id.btn_subscribe);
        }

        void bind(final VipPlan plan, final OnPlanSelectedListener listener) {
            tvName.setText(plan.getName());
            tvSubtitle.setText(plan.getSubtitle());
            tvPrice.setText(plan.getPrice());
            tvPriceDetail.setText(plan.getPriceDetail());

            // Badge de destaque
            if (plan.isPopular()) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText("MAIS POPULAR");
                tvBadge.setBackgroundResource(R.drawable.shape_badge_red);
                itemView.setBackgroundResource(R.drawable.shape_card_vip_popular);
            } else if (plan.isBestValue()) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText("MELHOR PREÇO");
                tvBadge.setBackgroundResource(R.drawable.shape_badge_gold);
                itemView.setBackgroundResource(R.drawable.shape_card_vip_gold);
            } else {
                tvBadge.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.shape_card_dark);
            }

            // Bônus de moedas
            if (plan.getBonusCoins() > 0) {
                tvBonusCoins.setVisibility(View.VISIBLE);
                tvBonusCoins.setText("🪙 +" + plan.getBonusCoins() + " moedas bônus");
            } else {
                tvBonusCoins.setVisibility(View.GONE);
            }

            // Lista de benefícios dinâmica
            llBenefits.removeAllViews();
            if (plan.getBenefits() != null) {
                for (String benefit : plan.getBenefits()) {
                    TextView tv = new TextView(itemView.getContext());
                    tv.setText("✓  " + benefit);
                    tv.setTextColor(0xFFBBBBBB);
                    tv.setTextSize(12f);
                    tv.setPadding(0, 4, 0, 4);
                    llBenefits.addView(tv);
                }
            }

            btnSubscribe.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.onPlanSelected(plan);
                }
            });
        }
    }
}
