/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Before;
import org.junit.Test;

import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.conclave.DeviceEventSource;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.log.JavaLog;
import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceCollectionTest {

    private final ResourceLoader mLoader = new ResourceLoader();

    private static class MockAferoClient implements AferoClient {

        private final ResourceLoader mLoader = new ResourceLoader();

        MockAferoClient() {
        }

        @Override
        public Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode) {
            return null;
        }

        @Override
        public Observable<RequestResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, DeviceRequest[] body, int maxRetryCount, int statusCode) {
            return null;
        }

        @Override
        public Observable<DeviceProfile> getDeviceProfile(String profileId, String locale, ImageSize imageSize) {
            return null;
        }

        @Override
        public Observable<DeviceProfile[]> getAccountDeviceProfiles(String locale, ImageSize imageSize) {
            return null;
        }

        @Override
        public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId) {
            return null;
        }

        @Override
        public Observable<Location> putDeviceLocation(String deviceId, Location location) {
            return null;
        }

        @Override
        public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
            return null;
        }

        @Override
        public Observable<DeviceAssociateResponse> deviceAssociateGetProfile(String associationId, boolean isOwnershipVerified, String locale, ImageSize imageSize) {
            return null;
        }

        @Override
        public Observable<DeviceAssociateResponse> deviceAssociate(String associationId) {
            return null;
        }

        @Override
        public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
            return null;
        }

        @Override
        public String getActiveAccountId() {
            return null;
        }

        @Override
        public int getStatusCode(Throwable t) {
            return 0;
        }

        @Override
        public boolean isTransferVerificationError(Throwable t) {
            return false;
        }
    }

    @Before
    public void beforeTests() {
        AfLog.init(new JavaLog());
    }

    private DeviceCollection makeDeviceCollection(DeviceEventSource source) {

        MockAferoClient aferoClient = new MockAferoClient();
        DeviceProfileCollection profileCollection = new DeviceProfileCollection(aferoClient, AferoClient.ImageSize.SIZE_3X, "mock-locale");

        return new DeviceCollection(source, profileCollection, aferoClient);
    }

    @Test
    public void start() throws Exception {
        MockDeviceEventSource messageSource = new MockDeviceEventSource();
        DeviceCollection deviceCollection = makeDeviceCollection(messageSource);

        assertEquals(false, messageSource.mSnapshotSubject.hasObservers());

        deviceCollection.start();

        assertEquals(true, messageSource.mSnapshotSubject.hasObservers());
    }

    @Test
    public void stop() throws Exception {
        MockDeviceEventSource messageSource = new MockDeviceEventSource();
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
        MockDeviceEventSource messageSource = new MockDeviceEventSource();
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