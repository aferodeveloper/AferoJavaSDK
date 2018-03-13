/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.log;

public class JavaLog implements AfLog.Impl {

    private String mTag = "AferoSDK";

    @Override
    public void setTag(String tag) {
        mTag = tag;
    }

    @Override
    public String getTag() {
        return mTag;
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
        System.out.println(s);
    }

    @Override
    public void d(String s) {
        System.out.println(s);
    }

    @Override
    public void i(String s) {
        System.out.println(s);
    }

    @Override
    public void w(String s) {
        System.out.println(s);
    }

    @Override
    public void e(String s) {
        System.err.println(s);
    }

    @Override
    public void e(Throwable t) {
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
