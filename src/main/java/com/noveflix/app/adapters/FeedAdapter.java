package com.noveflix.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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

    public FeedAdapter(Context context, List<Episode> episodes, OnEpisodeClickListener listener) {
        this.context  = context;
        this.episodes = episodes;
        this.listener = listener;
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

        final Episode episode = episodes.get(position);

        ImageView ivThumbnail  = (ImageView) convertView.findViewById(R.id.iv_thumbnail);
        ImageView ivPlayBtn    = (ImageView) convertView.findViewById(R.id.iv_play_btn);
        ImageView ivLockIcon   = (ImageView) convertView.findViewById(R.id.iv_lock_icon);
        ImageView ivLikeBtn    = (ImageView) convertView.findViewById(R.id.iv_like_btn);
        ImageView ivShareBtn   = (ImageView) convertView.findViewById(R.id.iv_share_btn);
        TextView  tvNovela     = (TextView)  convertView.findViewById(R.id.tv_novela_title);
        TextView  tvEpisode    = (TextView)  convertView.findViewById(R.id.tv_episode_info);
        TextView  tvDesc       = (TextView)  convertView.findViewById(R.id.tv_description);
        TextView  tvCountry    = (TextView)  convertView.findViewById(R.id.tv_country);
        TextView  tvLikeCount  = (TextView)  convertView.findViewById(R.id.tv_like_count);
        TextView  tvFreeTag    = (TextView)  convertView.findViewById(R.id.tv_free_tag);
        TextView  tvCoinCost   = (TextView)  convertView.findViewById(R.id.tv_coin_cost);
        View      overlayLocked = convertView.findViewById(R.id.overlay_locked);

        Glide.with(context)
                .load(episode.getThumbnailUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(ivThumbnail);

        tvNovela.setText(episode.getNovelaTitle());
        tvEpisode.setText("Ep. " + episode.getEpisodeNumber() + " · " + episode.getFormattedDuration());
        tvDesc.setText(episode.getDescription());
        tvCountry.setText(episode.getCountryFlag() + " " + getCountryName(episode.getCountryCode()));
        tvLikeCount.setText(episode.getFormattedLikes());
        ivLikeBtn.setImageResource(episode.isLiked() ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        ivPlayBtn.setVisibility(View.VISIBLE);

        if (episode.isFree()) {
            tvFreeTag.setVisibility(View.VISIBLE);
            tvCoinCost.setVisibility(View.GONE);
            overlayLocked.setVisibility(View.GONE);
            ivLockIcon.setVisibility(View.GONE);
        } else {
            tvFreeTag.setVisibility(View.GONE);
            tvCoinCost.setVisibility(View.VISIBLE);
            tvCoinCost.setText("🪙 2");
            overlayLocked.setVisibility(View.VISIBLE);
            ivLockIcon.setVisibility(View.VISIBLE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onEpisodeClick(episode);
            }
        });

        ivLikeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean newLiked = !episode.isLiked();
                episode.setLiked(newLiked);
                episode.setLikeCount(episode.getLikeCount() + (newLiked ? 1 : -1));
                notifyDataSetChanged();
            }
        });

        if (ivShareBtn != null) {
            ivShareBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // compartilhar via HomeFragment
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
