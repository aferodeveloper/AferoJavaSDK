/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client;

import java.util.concurrent.TimeUnit;

import io.afero.sdk.log.AfLog;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class AfHttpClient {

    private static OkHttpClient sClient;

    public static OkHttpClient create(HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {

        if (sClient == null) {
            sClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        AfLog.d(message);
                    }
                })
                .setLevel(logLevel))
                .connectTimeout(defaultTimeout, TimeUnit.SECONDS)
                .readTimeout(defaultTimeout, TimeUnit.SECONDS)
                .writeTimeout(defaultTimeout, TimeUnit.SECONDS)
                .build();
        }

        return sClient;
    }

    public static OkHttpClient get() {
        return sClient;
    }

}
