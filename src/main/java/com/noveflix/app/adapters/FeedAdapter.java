package com.noveflix.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v7.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noveflix.app.R;
import com.noveflix.app.models.Episode;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.EpisodeViewHolder> {

    public interface OnEpisodeActionListener {
        void onPlayEpisode(Episode episode);
        void onLikeEpisode(Episode episode, boolean liked);
        void onShareEpisode(Episode episode);
    }

    private final List<Episode>            episodes;
    private final OnEpisodeActionListener  listener;

    public FeedAdapter(List<Episode> episodes, OnEpisodeActionListener listener) {
        this.episodes = episodes;
        this.listener = listener;
    }

    @Override
    public EpisodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode_feed, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EpisodeViewHolder holder, int position) {
        holder.bind(episodes.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivThumbnail;
        private final ImageView ivPlayBtn;
        private final ImageView ivLockIcon;
        private final ImageView ivLikeBtn;
        private final ImageView ivShareBtn;
        private final TextView  tvNovelaTitle;
        private final TextView  tvEpisodeInfo;
        private final TextView  tvDescription;
        private final TextView  tvCountry;
        private final TextView  tvLikeCount;
        private final TextView  tvFreeTag;
        private final TextView  tvCoinCost;
        private final View      overlayLocked;

        EpisodeViewHolder(View itemView) {
            super(itemView);
            ivThumbnail   = itemView.findViewById(R.id.iv_thumbnail);
            ivPlayBtn     = itemView.findViewById(R.id.iv_play_btn);
            ivLockIcon    = itemView.findViewById(R.id.iv_lock_icon);
            ivLikeBtn     = itemView.findViewById(R.id.iv_like_btn);
            ivShareBtn    = itemView.findViewById(R.id.iv_share_btn);
            tvNovelaTitle = itemView.findViewById(R.id.tv_novela_title);
            tvEpisodeInfo = itemView.findViewById(R.id.tv_episode_info);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvCountry     = itemView.findViewById(R.id.tv_country);
            tvLikeCount   = itemView.findViewById(R.id.tv_like_count);
            tvFreeTag     = itemView.findViewById(R.id.tv_free_tag);
            tvCoinCost    = itemView.findViewById(R.id.tv_coin_cost);
            overlayLocked = itemView.findViewById(R.id.overlay_locked);
        }

        void bind(final Episode episode, final OnEpisodeActionListener listener) {
            Context ctx = itemView.getContext();

            // Thumbnail via Glide
            Glide.with(ctx)
                    .load(episode.getThumbnailUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(ivThumbnail);

            // Textos informativos
            tvNovelaTitle.setText(episode.getNovelaTitle());
            tvEpisodeInfo.setText("Ep. " + episode.getEpisodeNumber()
                    + " • " + episode.getFormattedDuration());
            tvDescription.setText(episode.getDescription());
            tvCountry.setText(episode.getCountryFlag() + " " + getCountryName(episode.getCountryCode()));
            tvLikeCount.setText(episode.getFormattedLikes());

            // Estado de like
            ivLikeBtn.setImageResource(episode.isLiked()
                    ? R.drawable.ic_heart : R.drawable.ic_heart_outline);

            // Bloqueio / gratuidade
            boolean free = episode.isFree();
            if (free) {
                tvFreeTag.setVisibility(View.VISIBLE);
                tvCoinCost.setVisibility(View.GONE);
                overlayLocked.setVisibility(View.GONE);
                ivLockIcon.setVisibility(View.GONE);
                ivPlayBtn.setVisibility(View.VISIBLE);
            } else {
                tvFreeTag.setVisibility(View.GONE);
                tvCoinCost.setVisibility(View.VISIBLE);
                tvCoinCost.setText("🪙 2");
                overlayLocked.setVisibility(View.VISIBLE);
                ivLockIcon.setVisibility(View.VISIBLE);
                ivPlayBtn.setVisibility(View.VISIBLE);
            }

            // Play / desbloqueio ao tocar
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onPlayEpisode(episode);
                }
            });
            ivPlayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onPlayEpisode(episode);
                }
            });

            // Like
            ivLikeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean newLiked = !episode.isLiked();
                    listener.onLikeEpisode(episode, newLiked);
                    ivLikeBtn.setImageResource(newLiked
                            ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
                    tvLikeCount.setText(episode.getFormattedLikes());
                }
            });

            // Compartilhar
            ivShareBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onShareEpisode(episode);
                }
            });
        }

        private String getCountryName(String code) {
            if ("JP".equals(code)) return "Jap\u00e3o";
            if ("KR".equals(code)) return "Coreia";
            if ("TR".equals(code)) return "Turquia";
            if ("MX".equals(code)) return "M\u00e9xico";
            if ("CN".equals(code)) return "China";
            return code;
        }
    }
}
