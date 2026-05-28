package com.example.mapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "marine_education_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_ROLE = "role";
    private static final String KEY_GUEST_MODE = "guest_mode";

    private final SharedPreferences preferences;
    private static PreferenceManager instance;

    private PreferenceManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    public void saveUser(int userId, String username, String nickname, String role) {
        preferences.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_NICKNAME, nickname)
                .putString(KEY_ROLE, role)
                .putBoolean(KEY_GUEST_MODE, false)
                .apply();
    }

    public int getUserId() {
        return preferences.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public String getNickname() {
        return preferences.getString(KEY_NICKNAME, "");
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, "");
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public boolean isGuestMode() {
        return preferences.getBoolean(KEY_GUEST_MODE, false);
    }

    public void setGuestMode(boolean guest) {
        if (guest) {
            preferences.edit()
                    .putBoolean(KEY_GUEST_MODE, true)
                    .remove(KEY_USER_ID)
                    .remove(KEY_USERNAME)
                    .remove(KEY_NICKNAME)
                    .remove(KEY_ROLE)
                    .apply();
        } else {
            preferences.edit().putBoolean(KEY_GUEST_MODE, false).apply();
        }
    }

    public void clearUser() {
        preferences.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_NICKNAME)
                .remove(KEY_ROLE)
                .putBoolean(KEY_GUEST_MODE, false)
                .apply();
    }
}
