package com.noveflix.app;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import android.support.v7.app.AppCompatActivity;

import com.noveflix.app.fragments.CatalogFragment;
import com.noveflix.app.fragments.HomeFragment;
import com.noveflix.app.fragments.ProfileFragment;
import com.noveflix.app.fragments.VipFragment;

public class MainActivity extends AppCompatActivity {

    private Fragment         activeFragment;
    private CatalogFragment catalogFragment = new CatalogFragment();
    private HomeFragment    homeFragment    = new HomeFragment();
    private VipFragment     vipFragment     = new VipFragment();
    private ProfileFragment profileFragment = new ProfileFragment();

    private LinearLayout btnTabCatalog;
    private LinearLayout btnTabHome;
    private LinearLayout btnTabVip;
    private LinearLayout btnTabProfile;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_main);

        btnTabCatalog = (LinearLayout) findViewById(R.id.btn_tab_catalog);
        btnTabHome    = (LinearLayout) findViewById(R.id.btn_tab_home);
        btnTabVip     = (LinearLayout) findViewById(R.id.btn_tab_vip);
        btnTabProfile = (LinearLayout) findViewById(R.id.btn_tab_profile);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, catalogFragment, "catalog")
                    .add(R.id.fragment_container, homeFragment, "home").hide(homeFragment)
                    .add(R.id.fragment_container, vipFragment, "vip").hide(vipFragment)
                    .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                    .commit();
            activeFragment = catalogFragment;
        }

        btnTabCatalog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFragment(catalogFragment);
            }
        });

        btnTabHome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFragment(homeFragment);
            }
        });

        btnTabVip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFragment(vipFragment);
            }
        });

        btnTabProfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFragment(profileFragment);
            }
        });
    }

    private void showFragment(Fragment target) {
        if (target == activeFragment) return;
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.hide(activeFragment);
        tx.show(target);
        tx.commit();
        activeFragment = target;
    }

    public void navigateToVip() {
        showFragment(vipFragment);
    }
}
