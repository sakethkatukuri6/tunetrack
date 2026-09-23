package com.example.mp3player.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREF_THEME = "app_theme_prefs";
    private static final String KEY_NIGHT_MODE = "saved_night_mode";

    public static int getSavedNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_THEME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void applySavedTheme(Context context) {
        int mode = getSavedNightMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void setNightMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_THEME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean isCurrentlyDark(Context context) {
        int mode = getSavedNightMode(context);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return false;

        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
