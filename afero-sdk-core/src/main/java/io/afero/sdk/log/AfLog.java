/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.log;

public class AfLog {

    private static Impl mImpl = new JavaLog();

    public static void init(AfLog.Impl impl) {
        mImpl = impl;
    }

    public static void setUserEmail(String email) {
        mImpl.setUserEmail(email);
    }

    public static void setUserId(String id) {
        mImpl.setUserId(id);
    }

    public static void setString(String key, String value) {
        mImpl.setString(key, value);
    }

    public static void i(String s) {
        mImpl.i(s);
    }

    public static void d(String s) {
        mImpl.d(s);
    }

    public static void w(String s) {
        mImpl.w(s);
    }

    public static void e(String s) {
        mImpl.e(s);
    }

    public static void e(Throwable t) {
        mImpl.e(t);
    }

    public static void content(String type) {
        mImpl.content(type);
    }

    public static void content(String type, String id) {
        mImpl.content(type, id);
    }

    public static void content(String type, String id, String name) {
        mImpl.content(type, id, name);
    }

    public interface Impl {
        void setUserEmail(String email);
        void setUserId(String email);
        void setString(String key, String value);
        void i(String s);
        void d(String s);
        void w(String s);
        void e(String s);
        void e(Throwable t);
        void content(String type);
        void content(String type, String id);
        void content(String type, String id, String name);
    }
}
