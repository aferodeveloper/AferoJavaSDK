/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import android.support.annotation.CallSuper;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import retrofit2.HttpException;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

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

        @CallSuper
        @Override
        public void onCompleted() {
            U strongRef = mRef.get();
            if (strongRef != null) {
                onCompleted(strongRef);
            }
        }

        @CallSuper
        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            U strongRef = mRef.get();
            if (strongRef != null) {
                onError(strongRef, e);
            }
        }

        @CallSuper
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

    private static class Retry {
        int retryCount;
        Throwable throwable;

        Retry(int r, Throwable t) {
            retryCount = r;
            throwable = t;
        }
    }

    public static class RetryOnError implements Func1<Observable<? extends Throwable>, Observable<?>> {

        private int mMaxRetryCount;
        private int mRetryOnStatus;

        public RetryOnError(int maxRetryCount, int retryOnStatus) {
            mMaxRetryCount = maxRetryCount;
            mRetryOnStatus = retryOnStatus;
        }

        public RetryOnError(int maxRetryCount) {
            this(maxRetryCount, 0);
        }

        @Override
        public Observable<?> call(Observable<? extends Throwable> observable) {
            return observable.zipWith(Observable.range(1, mMaxRetryCount), new Func2<Throwable, Integer, Retry>() {
                @Override
                public Retry call(Throwable throwable, Integer retry) {
                    return new Retry(retry, throwable);
                }
            })
            .flatMap(new Func1<Retry, Observable<?>>() {
                @Override
                public Observable<?> call(Retry retry) {
                    if (retry.throwable instanceof HttpException) {
                        int status = ((HttpException) retry.throwable).code();

                        AfLog.e("RetryOnError: retry=" + retry.retryCount + " '" + retry.throwable.getMessage() + "' status=" + status);

                        if (mRetryOnStatus == 0 || mRetryOnStatus == status) {
                            return Observable.timer(retry.retryCount, TimeUnit.SECONDS);
                        }
                    }

                    return Observable.error(retry.throwable);
                }
            });
        }
    }
}
