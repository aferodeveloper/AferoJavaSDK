/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import java.lang.ref.WeakReference;

import io.afero.sdk.log.AfLog;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RxUtils {

    public static Subscription safeUnSubscribe(Subscription subscription) {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        return null;
    }

    public static class IgnoreResponseObserver<T> implements rx.Observer<T> {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            AfLog.e(e);
        }

        @Override
        public void onNext(T response) {
        }
    }

    public static class Mapper<T,U> implements Func1<T, U> {

        private final U mObject;

        public Mapper(U o) {
            mObject = o;
        }

        @Override
        public U call(T o) {
            return mObject;
        }
    }

    public static class FlatMapper<T,U> implements Func1<T, Observable<U>> {

        private Observable<U> mObservable;

        public FlatMapper(Observable<U> o) {
            mObservable = o;
        }

        @Override
        public Observable<U> call(T t) {
            return mObservable;
        }
    }

    public static abstract class WeakObserver<T,U> implements rx.Observer<T> {
        private WeakReference<U> mRef;

        public WeakObserver(U strongRef) {
            mRef = new WeakReference<U>(strongRef);
        }

        public abstract void onCompleted(U strongRef);
        public abstract void onError(U strongRef, Throwable e);
        public abstract void onNext(U strongRef, T obj);

        @Override
        public void onCompleted() {
            U strongRef = mRef.get();
            if (strongRef != null) {
                onCompleted(strongRef);
            }
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            U strongRef = mRef.get();
            if (strongRef != null) {
                onError(strongRef, e);
            }
        }

        @Override
        public void onNext(T obj) {
            U strongRef = mRef.get();
            if (strongRef != null) {
                onNext(strongRef, obj);
            }
        }
    }

    public static abstract class WeakAction0<U> implements Action0 {
        private WeakReference<U> mRef;

        public WeakAction0(U strongRef) {
            mRef = new WeakReference<U>(strongRef);
        }

        public abstract void call(U strongRef);

        @Override
        public void call() {
            call(mRef.get());
        }
    }

    public static abstract class WeakAction1<T,U> implements Action1<T> {
        private WeakReference<U> mRef;

        public WeakAction1(U strongRef) {
            mRef = new WeakReference<U>(strongRef);
        }

        public abstract void call(U strongRef, T t);

        @Override
        public void call(T t) {
            call(mRef.get(), t);
        }
    }

    public static abstract class WeakFunc1<T,R,U> implements Func1<T,R> {
        private WeakReference<U> mRef;

        public WeakFunc1(U strongRef) {
            mRef = new WeakReference<U>(strongRef);
        }

        public abstract R call(U strongRef, T t);

        @Override
        public R call(T t) {
            return call(mRef.get(), t);
        }
    }

}
