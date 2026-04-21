package com.noveflix.app;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import android.support.v7.app.AppCompatActivity;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL     = "video_url";
    public static final String EXTRA_EPISODE_TITLE = "episode_title";
    public static final String EXTRA_NOVELA_TITLE  = "novela_title";
    public static final String EXTRA_EPISODE_NUM   = "episode_num";

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_player);

        String videoUrl    = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        String epTitle     = getIntent().getStringExtra(EXTRA_EPISODE_TITLE);
        String novelaTitle = getIntent().getStringExtra(EXTRA_NOVELA_TITLE);
        int    epNum       = getIntent().getIntExtra(EXTRA_EPISODE_NUM, 1);

        TextView    tvNovela  = (TextView)    findViewById(R.id.player_novela_title);
        TextView    tvEpisode = (TextView)    findViewById(R.id.player_episode_title);
        ImageButton btnBack   = (ImageButton) findViewById(R.id.player_back);

        tvNovela.setText(novelaTitle);
        tvEpisode.setText("Ep. " + epNum + " — " + epTitle);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        videoView = (VideoView) findViewById(R.id.player_view);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        if (videoUrl != null && !videoUrl.isEmpty()) {
            videoView.setVideoURI(Uri.parse(videoUrl));
            videoView.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}


public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL    = "video_url";
    public static final String EXTRA_EPISODE_TITLE = "episode_title";
    public static final String EXTRA_NOVELA_TITLE  = "novela_title";
    public static final String EXTRA_EPISODE_NUM   = "episode_num";

    private ExoPlayer player;
    private StyledPlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tela cheia imersiva para o player
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_player);

        String videoUrl    = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        String epTitle     = getIntent().getStringExtra(EXTRA_EPISODE_TITLE);
        String novelaTitle = getIntent().getStringExtra(EXTRA_NOVELA_TITLE);
        int    epNum       = getIntent().getIntExtra(EXTRA_EPISODE_NUM, 1);

        TextView tvNovela  = findViewById(R.id.player_novela_title);
        TextView tvEpisode = findViewById(R.id.player_episode_title);
        ImageButton btnBack = findViewById(R.id.player_back);

        tvNovela.setText(novelaTitle);
        tvEpisode.setText("Ep. " + epNum + " — " + epTitle);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        playerView = findViewById(R.id.player_view);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        if (videoUrl != null && !videoUrl.isEmpty()) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)));
            player.prepare();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
