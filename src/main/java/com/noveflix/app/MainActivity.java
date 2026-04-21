package com.noveflix.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.noveflix.app.fragments.HomeFragment;
import com.noveflix.app.fragments.ProfileFragment;
import com.noveflix.app.fragments.VipFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment activeFragment;

    private final HomeFragment    homeFragment    = new HomeFragment();
    private final VipFragment     vipFragment     = new VipFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Barra de status transparente
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Carrega todos os fragmentos (oculta todos exceto Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, homeFragment,    "home")
                    .add(R.id.fragment_container, vipFragment,     "vip").hide(vipFragment)
                    .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                    .commit();
            activeFragment = homeFragment;
        }

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    showFragment(homeFragment);
                } else if (id == R.id.nav_vip) {
                    showFragment(vipFragment);
                } else if (id == R.id.nav_profile) {
                    showFragment(profileFragment);
                }
                return true;
            }
        });
    }

    private void showFragment(@NonNull Fragment target) {
        if (target == activeFragment) return;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.hide(activeFragment);
        tx.show(target);
        tx.commit();
        activeFragment = target;
    }

    /** Muda para aba VIP a partir de qualquer fragment */
    public void navigateToVip() {
        bottomNav.setSelectedItemId(R.id.nav_vip);
    }
}
