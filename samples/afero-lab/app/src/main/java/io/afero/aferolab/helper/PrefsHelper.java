/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;

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

    public static AccessToken getAccessToken(Context context) {
        try {
            String tokenJsonString = getAccountSharedPrefs(context).getString(ACCOUNT_PREFS_ACCESS_TOKEN, "");
            ObjectMapper mapper = new ObjectMapper();
            AccessToken token = mapper.readValue(tokenJsonString, AccessToken.class);
            AfLog.d("#### Get token from prefs: " + token);

            return token;
        } catch (IOException e) {
            AfLog.d("Unable to get access token " + e.toString());
            return null;
        }
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



    public static void saveAccessToken(Context context, AccessToken token) {
        try {
            //Convert token to JSON string
            ObjectMapper mapper = new ObjectMapper();
            String tokenJsonString = mapper.writeValueAsString(token);
            AfLog.d("####  Save Token to prefs: " + tokenJsonString);
            getAccountSharedPrefs(context).edit()
            .putString(ACCOUNT_PREFS_ACCESS_TOKEN, tokenJsonString)
            .apply();
        } catch (Exception e) {
            AfLog.d("Unable to save access token " + e.toString());
        }
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
