/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.android.log;

import android.util.Log;

import io.afero.sdk.log.AfLog;


public class AndroidLog implements AfLog.Impl {

    public AndroidLog() {
        this("");
    }

    public AndroidLog(String tag) {
        AfLog.setTag(tag);
    }

    @Override
    public void setUserEmail(String email) {

    }

    @Override
    public void setUserId(String email) {

    }

    @Override
    public void setString(String key, String value) {

    }

    @Override
    public void v(String s) {
        Log.v(AfLog.getTag(), s);
    }

    @Override
    public void d(String s) {
        Log.d(AfLog.getTag(), s);
    }

    @Override
    public void i(String s) {
        Log.i(AfLog.getTag(), s);
    }

    @Override
    public void w(String s) {
        Log.w(AfLog.getTag(), s);
    }

    @Override
    public void e(String s) {
        Log.e(AfLog.getTag(), s);
    }

    @Override
    public void e(Throwable t) {
        Log.e(AfLog.getTag(), t.getMessage());
        t.printStackTrace();
    }

    @Override
    public void content(String type) {

    }

    @Override
    public void content(String type, String id) {

    }

    @Override
    public void content(String type, String id, String name) {

    }
}
