package com.androapplite.shadowsocks.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by jim on 16/4/27.
 */
public class DefaultSharedPrefeencesUtil {
    public static final SharedPreferences getDefaultSharedPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static final SharedPreferences.Editor getDefaultSharedPreferencesEditor(Context context){
        return getDefaultSharedPreferences(context).edit();
    }

    public static final boolean isNewUser(Context context){
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.IS_NEW_USER, true);
    }

    public static final void markAsOldUser(Context context){
        final SharedPreferences.Editor editor = getDefaultSharedPreferencesEditor(context);
        editor.putBoolean(SharedPreferenceKey.IS_NEW_USER, false).apply();
    }

    public static final boolean isRateUsFragmentShown(Context context){
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, false);
    }

    public static final void markRateUsFragmentAsShowed(Context context){
        final SharedPreferences.Editor editor = getDefaultSharedPreferencesEditor(context);
        editor.putBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, true).apply();

    }
}