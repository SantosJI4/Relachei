package com.noveflix.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.noveflix.app.utils.PrefsManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tela cheia imersiva
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        // Verifica primeiro acesso e distribui moedas
        PrefsManager prefs = PrefsManager.getInstance(this);
        final boolean isFirstTime = prefs.isFirstLaunch();
        if (isFirstTime) {
            prefs.setCoins(10);
            prefs.setFirstLaunchDone();
        }

        // Anima o logo com fade in
        View logo      = findViewById(R.id.splash_logo);
        TextView tagline = findViewById(R.id.splash_tagline);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(900);
        fadeIn.setFillAfter(true);
        logo.startAnimation(fadeIn);

        AlphaAnimation fadeInSlow = new AlphaAnimation(0f, 1f);
        fadeInSlow.setDuration(1400);
        fadeInSlow.setStartOffset(500);
        fadeInSlow.setFillAfter(true);
        tagline.startAnimation(fadeInSlow);

        // Navega para MainActivity após delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();

                if (isFirstTime) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.welcome_coins), Toast.LENGTH_LONG).show();
                }
            }
        }, SPLASH_DELAY);
    }
}
