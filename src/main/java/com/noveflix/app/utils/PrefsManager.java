package com.noveflix.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.noveflix.app.models.VipPlan;

public class PrefsManager {
    private static final String PREF_NAME         = "noveflix_prefs";
    private static final String KEY_COINS         = "user_coins";
    private static final String KEY_IS_VIP        = "is_vip";
    private static final String KEY_VIP_EXPIRY    = "vip_expiry";
    private static final String KEY_VIP_TYPE      = "vip_type";
    private static final String KEY_VIP_NOVELAS   = "vip_novelas_remaining";
    private static final String KEY_FIRST_LAUNCH  = "first_launch";
    private static final String KEY_EPISODES_WATCHED = "episodes_watched";
    private static final String KEY_NOVELAS_SAVED = "novelas_saved";

    private static PrefsManager instance;
    private final SharedPreferences prefs;

    private PrefsManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static PrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefsManager(context);
            instance.initFirstLaunch();
        }
        return instance;
    }

    // Dá 25 moedas de boas-vindas apenas no primeiro launch
    private void initFirstLaunch() {
        if (!prefs.getBoolean(KEY_FIRST_LAUNCH, false)) {
            prefs.edit()
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .putInt(KEY_COINS, 25)
                .apply();
        }
    }

    // === MOEDAS ===
    public int getCoins() {
        return prefs.getInt(KEY_COINS, 25);
    }

    public void setCoins(int amount) {
        prefs.edit().putInt(KEY_COINS, amount).apply();
    }

    public void addCoins(int amount) {
        setCoins(getCoins() + amount);
    }

    public boolean spendCoins(int amount) {
        int current = getCoins();
        if (current >= amount) {
            setCoins(current - amount);
            return true;
        }
        return false;
    }

    // === VIP ===
    public boolean isVipActive() {
        if (!prefs.getBoolean(KEY_IS_VIP, false)) return false;
        String type = getVipType();
        if (VipPlan.TYPE_PACK.equals(type)) {
            return getRemainingVipNovelas() > 0;
        }
        long expiry = getVipExpiry();
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            clearVip();
            return false;
        }
        return true;
    }

    public void activateVip(String type, long durationMillis, int novelaPacks) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_VIP, true);
        editor.putString(KEY_VIP_TYPE, type);
        if (VipPlan.TYPE_PACK.equals(type)) {
            editor.putInt(KEY_VIP_NOVELAS, novelaPacks);
            editor.putLong(KEY_VIP_EXPIRY, -1);
        } else {
            editor.putLong(KEY_VIP_EXPIRY, System.currentTimeMillis() + durationMillis);
            editor.putInt(KEY_VIP_NOVELAS, 0);
        }
        editor.apply();
    }

    public void clearVip() {
        prefs.edit()
             .putBoolean(KEY_IS_VIP, false)
             .putLong(KEY_VIP_EXPIRY, 0)
             .putString(KEY_VIP_TYPE, "")
             .putInt(KEY_VIP_NOVELAS, 0)
             .apply();
    }

    public long getVipExpiry() {
        return prefs.getLong(KEY_VIP_EXPIRY, 0);
    }

    public String getVipType() {
        return prefs.getString(KEY_VIP_TYPE, "");
    }

    public int getRemainingVipNovelas() {
        return prefs.getInt(KEY_VIP_NOVELAS, 0);
    }

    public void decrementVipNovelas() {
        int remaining = getRemainingVipNovelas();
        if (remaining > 0) {
            prefs.edit().putInt(KEY_VIP_NOVELAS, remaining - 1).apply();
        }
    }

    // === PRIMEIRO LANÇAMENTO ===
    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    // === ESTATÍSTICAS ===
    public int getEpisodesWatched() {
        return prefs.getInt(KEY_EPISODES_WATCHED, 0);
    }

    public void incrementEpisodesWatched() {
        prefs.edit().putInt(KEY_EPISODES_WATCHED, getEpisodesWatched() + 1).apply();
    }

    public int getNovelasSaved() {
        return prefs.getInt(KEY_NOVELAS_SAVED, 0);
    }

    public void setNovelasSaved(int count) {
        prefs.edit().putInt(KEY_NOVELAS_SAVED, count).apply();
    }
}
