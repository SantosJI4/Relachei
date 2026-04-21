package com.noveflix.app.adapters;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.noveflix.app.R;
import com.noveflix.app.models.Episode;

import java.util.List;

public class FeedAdapter extends BaseAdapter {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode);
    }

    private final Context               context;
    private final List<Episode>         episodes;
    private final OnEpisodeClickListener listener;
    private int screenHeight = 0;

    public FeedAdapter(Context context, List<Episode> episodes, OnEpisodeClickListener listener) {
        this.context  = context;
        this.episodes = episodes;
        this.listener = listener;
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(dm);
        screenHeight = dm.heightPixels;
    }

    public int getCount() {
        return episodes.size();
    }

    public Object getItem(int position) {
        return episodes.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_episode_feed, parent, false);
        }

        // Forçar altura de tela cheia (estilo TikTok)
        ViewGroup.LayoutParams lp = convertView.getLayoutParams();
        lp.height = screenHeight;
        convertView.setLayoutParams(lp);

        final Episode episode = episodes.get(position);

        VideoView vvEpisode   = (VideoView) convertView.findViewById(R.id.vv_episode);
        ImageView ivLockIcon  = (ImageView) convertView.findViewById(R.id.iv_lock_icon);
        ImageView ivLikeBtn   = (ImageView) convertView.findViewById(R.id.iv_like_btn);
        ImageView ivShareBtn  = (ImageView) convertView.findViewById(R.id.iv_share_btn);
        TextView  tvNovela    = (TextView)  convertView.findViewById(R.id.tv_novela_title);
        TextView  tvEpisode   = (TextView)  convertView.findViewById(R.id.tv_episode_info);
        TextView  tvDesc      = (TextView)  convertView.findViewById(R.id.tv_description);
        TextView  tvCountry   = (TextView)  convertView.findViewById(R.id.tv_country);
        TextView  tvLikeCount = (TextView)  convertView.findViewById(R.id.tv_like_count);
        TextView  tvFreeTag   = (TextView)  convertView.findViewById(R.id.tv_free_tag);
        TextView  tvCoinCost  = (TextView)  convertView.findViewById(R.id.tv_coin_cost);
        View overlayLocked    = convertView.findViewById(R.id.overlay_locked);

        if (tvNovela  != null) tvNovela.setText(episode.getNovelaTitle());
        if (tvEpisode != null) tvEpisode.setText("Ep. " + episode.getEpisodeNumber() + " · " + episode.getFormattedDuration());
        if (tvDesc    != null) tvDesc.setText(episode.getDescription());
        if (tvCountry != null) tvCountry.setText(episode.getCountryFlag() + " " + getCountryName(episode.getCountryCode()));
        if (tvLikeCount != null) tvLikeCount.setText(episode.getFormattedLikes());
        if (ivLikeBtn != null) ivLikeBtn.setImageResource(episode.isLiked() ? R.drawable.ic_heart : R.drawable.ic_heart_outline);

        if (episode.isFree()) {
            if (tvFreeTag    != null) tvFreeTag.setVisibility(View.VISIBLE);
            if (tvCoinCost   != null) tvCoinCost.setVisibility(View.GONE);
            if (overlayLocked != null) overlayLocked.setVisibility(View.GONE);
            if (ivLockIcon   != null) ivLockIcon.setVisibility(View.GONE);
        } else {
            if (tvFreeTag    != null) tvFreeTag.setVisibility(View.GONE);
            if (tvCoinCost   != null) { tvCoinCost.setVisibility(View.VISIBLE); tvCoinCost.setText("\uD83E\uDE99 2"); }
            if (overlayLocked != null) overlayLocked.setVisibility(View.VISIBLE);
            if (ivLockIcon   != null) ivLockIcon.setVisibility(View.VISIBLE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onEpisodeClick(episode);
            }
        });

        if (ivLikeBtn != null) {
            ivLikeBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    boolean newLiked = !episode.isLiked();
                    episode.setLiked(newLiked);
                    episode.setLikeCount(episode.getLikeCount() + (newLiked ? 1 : -1));
                    notifyDataSetChanged();
                }
            });
        }

        return convertView;
    }

    private String getCountryName(String code) {
        if ("JP".equals(code)) return "Japao";
        if ("KR".equals(code)) return "Coreia";
        if ("TR".equals(code)) return "Turquia";
        if ("MX".equals(code)) return "Mexico";
        if ("CN".equals(code)) return "China";
        return code;
    }
}
