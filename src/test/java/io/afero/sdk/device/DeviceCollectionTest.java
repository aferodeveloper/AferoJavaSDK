/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import io.afero.sdk.BuildConfig;
import io.afero.sdk.MockConclaveMessageSource;
import io.afero.sdk.ResourceLoader;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.api.AferoClientAPI;
import io.afero.sdk.client.afero.api.MockAferoClientAPI;
import io.afero.sdk.conclave.ConclaveMessageSource;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.log.JavaLog;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class DeviceCollectionTest {

    private final ResourceLoader mLoader = new ResourceLoader();

    static class MockAferoClient extends AferoClient {

        private final ResourceLoader mLoader = new ResourceLoader();

        MockAferoClient(String baseUrl, HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {
            super(baseUrl, logLevel, defaultTimeout);
        }

        @Override
        protected AferoClientAPI createAdapter(String baseUrl, HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create()) // synchronous
                    .addConverterFactory(JacksonConverterFactory.create(mLoader.objectMapper))
                    .build();

            // Create a MockRetrofit object with a NetworkBehavior which manages the fake behavior of calls.
            NetworkBehavior behavior = NetworkBehavior.create();
            behavior.setDelay(0, TimeUnit.MILLISECONDS);
            behavior.setErrorPercent(0);
            behavior.setFailurePercent(0);
            behavior.setVariancePercent(0);

            MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit)
                    .networkBehavior(behavior)
                    .build();

            BehaviorDelegate<AferoClientAPI> delegate = mockRetrofit.create(AferoClientAPI.class);
            return new MockAferoClientAPI(delegate);
        }
    }

    @Before
    public void beforeTests() {
        AfLog.init(new JavaLog());
    }

    private DeviceCollection makeDeviceCollection(ConclaveMessageSource source) {

        MockAferoClient aferoClient = new MockAferoClient("http://mock.afero.io", HttpLoggingInterceptor.Level.BASIC, 20);
        DeviceProfileCollection profileCollection = new DeviceProfileCollection(aferoClient, AferoClientAPI.ImageSize.SIZE_3X, "mock-locale");

        return new DeviceCollection(source, profileCollection, aferoClient);
    }

    @Test
    public void start() throws Exception {
        MockConclaveMessageSource messageSource = new MockConclaveMessageSource();
        DeviceCollection deviceCollection = makeDeviceCollection(messageSource);

        assertEquals(false, messageSource.mSnapshotSubject.hasObservers());

        deviceCollection.start();

        assertEquals(true, messageSource.mSnapshotSubject.hasObservers());
    }

    @Test
    public void stop() throws Exception {
        MockConclaveMessageSource messageSource = new MockConclaveMessageSource();
        DeviceCollection deviceCollection = makeDeviceCollection(messageSource);
        deviceCollection.start();

        assertEquals(true, messageSource.mSnapshotSubject.hasObservers());

        deviceCollection.stop();

        assertEquals(false, messageSource.mSnapshotSubject.hasObservers());
    }

    @Test
    public void addOrUpdate() throws Exception {

    }

    @Test
    public void deleteDevice() throws Exception {

    }

    @Test
    public void reset() throws Exception {

    }

    @Test
    public void getDevices() throws Exception {

    }

    @Test
    public void observeCreates() throws Exception {
        MockConclaveMessageSource messageSource = new MockConclaveMessageSource();
        DeviceCollection deviceCollection = makeDeviceCollection(messageSource);
        deviceCollection.start();

        DeviceSync[] snapshot = mLoader.createObjectFromJSONResource("deviceCollection/deviceSync.json", DeviceSync[].class);
        messageSource.putSnapshot(snapshot);

        assertEquals(1, deviceCollection.getCount());

        DeviceModel deviceModel = deviceCollection.getModel(snapshot[0].id);
        assertNotNull(deviceModel);

        DeviceProfile deviceProfile = deviceModel.getProfile();
        assertNotNull(deviceProfile);

        assertEquals(snapshot[0].profileId, deviceProfile.getId());
    }

    @Test
    public void observeProfileChanges() throws Exception {

    }

    @Test
    public void observeSnapshots() throws Exception {

    }

    @Test
    public void observeDeletes() throws Exception {

    }

    @Test
    public void hasUnAvailableDevices() throws Exception {

    }

    @Test
    public void hasAnyUserDevices() throws Exception {

    }

    @Test
    public void contains() throws Exception {

    }

    @Test
    public void getCount() throws Exception {

    }

    @Test
    public void getModelAt() throws Exception {

    }

    @Test
    public void getModel() throws Exception {

    }

}