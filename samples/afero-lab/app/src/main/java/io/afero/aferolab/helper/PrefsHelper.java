/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsHelper {

    private static final String SHARED_PREFS_ACCOUNT = "shared_prefs_account";

    private static final String ACCOUNT_PREFS_ACCESS_TOKEN = "prefs_access_token";
    private static final String ACCOUNT_PREFS_REFRESH_TOKEN = "prefs_refresh_token";
    private static final String ACCOUNT_PREFS_ACCOUNT_ID = "prefs_account_id";
    private static final String ACCOUNT_PREFS_USER_ID = "prefs_user_id";
    private static final String ACCOUNT_PREFS_ACCOUNT_NAME = "prefs_account_name";

    private static SharedPreferences getAccountSharedPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_ACCOUNT, Context.MODE_PRIVATE);
    }

    public static String getAccessToken(Context context) {
        return getAccountSharedPrefs(context).getString(ACCOUNT_PREFS_ACCESS_TOKEN, "");
    }

    public static String getRefreshToken(Context context) {
        return getAccountSharedPrefs(context).getString(ACCOUNT_PREFS_REFRESH_TOKEN, "");
    }

    public static String getUserId(Context context) {
        return getAccountSharedPrefs(context).getString(ACCOUNT_PREFS_USER_ID, "");
    }

    public static String getAccountId(Context context) {
        return getAccountSharedPrefs(context).getString(ACCOUNT_PREFS_ACCOUNT_ID, "");
    }

    public static String getAccountName(Context context) {
        return getAccountSharedPrefs(context).getString(ACCOUNT_PREFS_ACCOUNT_NAME, "");
    }

    public static void saveAccessToken(Context context, String token) {
        getAccountSharedPrefs(context).edit()
            .putString(ACCOUNT_PREFS_ACCESS_TOKEN, token)
            .apply();
    }

    public static void saveRefreshToken(Context context, String token) {
        getAccountSharedPrefs(context).edit()
            .putString(ACCOUNT_PREFS_REFRESH_TOKEN, token)
            .apply();
    }

    public static void saveAccountId(Context context, String id) {
        getAccountSharedPrefs(context).edit()
            .putString(ACCOUNT_PREFS_ACCOUNT_ID, id)
            .apply();
    }

    public static void saveAccountName(Context context, String name) {
        getAccountSharedPrefs(context).edit()
            .putString(ACCOUNT_PREFS_ACCOUNT_NAME, name)
            .apply();
    }

    public static void saveUserId(Context context, String id) {
        getAccountSharedPrefs(context).edit()
            .putString(ACCOUNT_PREFS_USER_ID, id)
            .apply();
    }

    public static void clearAccountPrefs(Context context) {
        getAccountSharedPrefs(context).edit()
            .remove(ACCOUNT_PREFS_ACCOUNT_ID)
            .remove(ACCOUNT_PREFS_USER_ID)
            .remove(ACCOUNT_PREFS_ACCESS_TOKEN)
            .remove(ACCOUNT_PREFS_REFRESH_TOKEN)
            .apply();
    }
}
