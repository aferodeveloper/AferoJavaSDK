/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.log;

public class AfLog {

    private static Impl mImpl = new JavaLog();
    private static String mTag = "";
    private static FilterLevel mFilterLevel = FilterLevel.VERBOSE;

    public enum FilterLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        SILENT
    };

    public static void init(AfLog.Impl impl) {
        mImpl = impl;
        mTag = "";
        mFilterLevel = FilterLevel.VERBOSE;
    }

    public static void setTag(String tag) {
        mTag = tag;
    }

    public static String getTag() {
        return mTag;
    }

    public static void setFilterLevel(FilterLevel level) {
        if (level != null) {
            mFilterLevel = level;
        }
    }

    public static FilterLevel getFilterLevel() {
        return mFilterLevel;
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

    public static void v(String s) {
        if (passesFilter(FilterLevel.VERBOSE)) {
            mImpl.v(s);
        }
    }

    public static void d(String s) {
        if (passesFilter(FilterLevel.DEBUG)) {
            mImpl.d(s);
        }
    }

    public static void i(String s) {
        if (passesFilter(FilterLevel.INFO)) {
            mImpl.i(s);
        }
    }

    public static void w(String s) {
        if (passesFilter(FilterLevel.WARNING)) {
            mImpl.w(s);
        }
    }

    public static void e(String s) {
        if (passesFilter(FilterLevel.ERROR)) {
            mImpl.e(s);
        }
    }

    public static void e(Throwable t) {
        if (passesFilter(FilterLevel.ERROR)) {
            mImpl.e(t);
        }
    }

    private static boolean passesFilter(FilterLevel level) {
        return mFilterLevel.compareTo(level) <= 0;
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

    static Impl getImpl() {
        return mImpl;
    }

    public interface Impl {
        void setUserEmail(String email);
        void setUserId(String email);
        void setString(String key, String value);
        void v(String s);
        void d(String s);
        void i(String s);
        void w(String s);
        void e(String s);
        void e(Throwable t);
        void content(String type);
        void content(String type, String id);
        void content(String type, String id, String name);
    }
}
