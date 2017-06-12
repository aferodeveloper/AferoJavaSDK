/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import android.util.Log;

class AndroidLog implements AfLog.Impl {

    private String mTag = "";

    public AndroidLog(String tag) {
        setTag(tag);
    }

    public void setTag(String tag) {
        mTag = tag;
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
    public void i(String s) {
        Log.i(mTag, s);
    }

    @Override
    public void d(String s) {
        Log.d(mTag, s);
    }

    @Override
    public void w(String s) {
        Log.w(mTag, s);
    }

    @Override
    public void e(String s) {
        Log.e(mTag, s);
    }

    @Override
    public void e(Throwable t) {
        Log.e(mTag, t.getMessage());
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
