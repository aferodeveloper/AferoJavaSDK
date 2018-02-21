/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.log;

public class AfLog {

    private static Impl mImpl;
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
        mFilterLevel = FilterLevel.VERBOSE;
    }

    public static void setTag(String tag) {
        getImpl().setTag(tag);
    }

    public static String getTag() {
        return getImpl().getTag();
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
        getImpl().setUserEmail(email);
    }

    public static void setUserId(String id) {
        getImpl().setUserId(id);
    }

    public static void setString(String key, String value) {
        getImpl().setString(key, value);
    }

    public static void v(String s) {
        if (passesFilter(FilterLevel.VERBOSE)) {
            getImpl().v(s);
        }
    }

    public static void d(String s) {
        if (passesFilter(FilterLevel.DEBUG)) {
            getImpl().d(s);
        }
    }

    public static void i(String s) {
        if (passesFilter(FilterLevel.INFO)) {
            getImpl().i(s);
        }
    }

    public static void w(String s) {
        if (passesFilter(FilterLevel.WARNING)) {
            getImpl().w(s);
        }
    }

    public static void e(String s) {
        if (passesFilter(FilterLevel.ERROR)) {
            getImpl().e(s);
        }
    }

    public static void e(Throwable t) {
        if (passesFilter(FilterLevel.ERROR)) {
            getImpl().e(t);
        }
    }

    private static boolean passesFilter(FilterLevel level) {
        return mFilterLevel.compareTo(level) <= 0;
    }

    public static void content(String type) {
        getImpl().content(type);
    }

    public static void content(String type, String id) {
        getImpl().content(type, id);
    }

    public static void content(String type, String id, String name) {
        getImpl().content(type, id, name);
    }

    static Impl getImpl() {
        if (mImpl == null) {
            mImpl = new JavaLog();
        }
        return mImpl;
    }

    public interface Impl {
        void setTag(String tag);
        String getTag();
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
