/*
 * Copyright (c) 2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class ClientID {
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static final String PREF_ID_WAS_REGISTERED = "PREF_ID_WAS_REGISTERED";

    private static String sClientID;
    private static boolean sIDWasRegistered;
    private static boolean sGeneratedNewClientID;

    public synchronized static String get(Context context) {
        if (sClientID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            sClientID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (sClientID == null) {
                sClientID = UUID.randomUUID().toString();
                sGeneratedNewClientID = true;
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, sClientID);
                editor.commit();
            }
        }
        return sClientID;
    }

    public static void setIDWasRegistered(boolean value) {
        sIDWasRegistered = value;
    }

    public static boolean getIDWasRegistered() {
        return sIDWasRegistered;
    }
}
