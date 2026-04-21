package com.noveflix.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.noveflix.app.R;
import com.noveflix.app.models.Episode;

import java.util.List;

public class EpisodeListAdapter extends BaseAdapter {

    public interface OnPlayListener {
        void onPlay(Episode episode);
    }

    private final Context context;
    private final List<Episode> episodes;
    private final OnPlayListener listener;

    public EpisodeListAdapter(Context context, List<Episode> episodes, OnPlayListener listener) {
        this.context = context;
        this.episodes = episodes;
        this.listener = listener;
    }

    public int getCount() { return episodes != null ? episodes.size() : 0; }
    public Object getItem(int pos) { return episodes.get(pos); }
    public long getItemId(int pos) { return pos; }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_episode_list, parent, false);
        }

        final Episode episode = episodes.get(position);

        TextView tvBadge    = (TextView)  convertView.findViewById(R.id.tv_ep_number_badge);
        TextView tvTitle    = (TextView)  convertView.findViewById(R.id.tv_ep_title);
        TextView tvDuration = (TextView)  convertView.findViewById(R.id.tv_ep_duration);
        TextView tvCost     = (TextView)  convertView.findViewById(R.id.tv_ep_cost);
        ImageView ivPlay    = (ImageView) convertView.findViewById(R.id.iv_play_ep);

        if (tvBadge    != null) tvBadge.setText(String.valueOf(episode.getEpisodeNumber()));
        if (tvTitle    != null) tvTitle.setText(episode.getTitle());
        if (tvDuration != null) tvDuration.setText(episode.getFormattedDuration());

        if (tvCost != null) {
            if (episode.isFree()) {
                tvCost.setText("GRÁTIS");
                tvCost.setBackgroundResource(R.drawable.shape_badge_red);
                tvCost.setTextColor(tvCost.getResources().getColor(R.color.text_primary));
            } else {
                tvCost.setText("\uD83E\uDE99 " + episode.getCoinCost());
                tvCost.setBackgroundResource(R.drawable.shape_badge_gold);
                tvCost.setTextColor(tvCost.getResources().getColor(android.R.color.black));
            }
        }

        View.OnClickListener playClick = new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) listener.onPlay(episode);
            }
        };

        if (ivPlay != null) ivPlay.setOnClickListener(playClick);
        convertView.setOnClickListener(playClick);

        return convertView;
    }
}
