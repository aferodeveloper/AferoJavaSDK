/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Test;

import java.io.IOException;
import java.util.TimeZone;
import java.util.Vector;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.InvalidateMessage;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeviceCollectionTest {

    @Test
    public void start() throws Exception {
        newDeviceCollectionTester()
                .verifyDeviceEventSourceHasNoObservers()

                // start should subscribe to device event source
                .deviceCollectionStart()
                .verifyDeviceEventSourceHasOneObserver()

                // start again should throw
                .deviceCollectionStartWithCatch()
                .verifyThrownIllegalStateException()
                ;
    }

    @Test
    public void stop() throws Exception {
        newDeviceCollectionTester()
                // stop should throw if called before start
                .deviceCollectionStopWithCatch()
                .verifyThrownIllegalStateException()
        ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .verifyDeviceEventSourceHasOneObserver()

                // stop should unsubscribe from device event source
                .deviceCollectionStop()
                .verifyDeviceEventSourceHasNoObservers()

                // stop again should throw
                .deviceCollectionStopWithCatch()
                .verifyThrownIllegalStateException()
                ;
    }

    @Test
    public void addDevice() throws Exception {
        newDeviceCollectionTester()
                // addDevice should throw if called before start
                .deviceCollectionAddDeviceWithCatch()
                .verifyThrownIllegalStateException()
        ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionAddDevice()

                .verifyAddDeviceReturnedDeviceModel()
                .verifyGetCountReturnsExpectedCount(1)
                .verifyGetDeviceReturnsNonNull("device-id")
        ;
    }

    @Test
    public void removeDevice() throws Exception {
        newDeviceCollectionTester()
                // removeDevice should throw if called before start
                .deviceCollectionRemoveDeviceWithCatch()
                .verifyThrownIllegalStateException()
        ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionAddDevice()

                .verifyGetDeviceReturnsNonNull("device-id")

                .deviceCollectionRemoveDevice("device-id")

                .verifyGetDeviceReturnsNull("device-id")
        ;

    }

    @Test
    public void getDevices() throws Exception {
        newDeviceCollectionTester()
                // getDevices should throw if called before start
                .deviceCollectionGetDevicesWithCatch()
                .verifyThrownIllegalStateException()
        ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionAddDevice()

                .verifyAddDeviceReturnedDeviceModel()
                .verifyGetCountReturnsExpectedCount(1)
                .verifyGetDeviceReturnsNonNull("device-id")
        ;
    }

    @Test
    public void reset() throws Exception {
        newDeviceCollectionTester()
                // reset should throw if called before start
                .deviceCollectionResetWithCatch()
                .verifyThrownIllegalStateException()
                ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionAddDevice()

                .verifyGetCountReturnsExpectedCount(1)

                .deviceCollectionReset()

                .verifyGetCountReturnsExpectedCount(0)
                ;
    }

    @Test
    public void observeCreates() throws Exception {
        newDeviceCollectionTester()
                // observeCreates should throw if called before start
                .deviceCollectionObserveCreatesWithCatch()
                .verifyThrownIllegalStateException()
                ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionObserveCreates()

                .deviceEventSourceSnapshot()

                .verifyObservedCreateCount(1)
        ;
    }

    @Test
    public void observeProfileChanges() throws Exception {
        newDeviceCollectionTester()
                // observeProfileChanges should throw if called before start
                .deviceCollectionObserveProfileChangesWithCatch()
                .verifyThrownIllegalStateException()
        ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionObserveProfileChanges()
                .deviceEventSourceSnapshot()

                .deviceEventSourceProfileChange()

                .verifyObservedProfileChangeCount(1)
        ;

    }

    @Test
    public void observeSnapshots() throws Exception {
        newDeviceCollectionTester()
                // observeSnapshots should throw if called before start
                .deviceCollectionObserveSnapshotsWithCatch()
                .verifyThrownIllegalStateException()
        ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionObserveSnapshots()

                .deviceEventSourceSnapshot()

                .verifyObservedSnapshotCount(1)
        ;
    }

    @Test
    public void observeDeletes() throws Exception {
        newDeviceCollectionTester()
                // observeDeletes should throw if called before start
                .deviceCollectionObserveDeletesWithCatch()
                .verifyThrownIllegalStateException()
        ;

        // test deletes emitted in response to snapshots
        newDeviceCollectionTester()
                .deviceCollectionStart()

                .deviceCollectionObserveDeletes()

                .deviceEventSourceSnapshot("snapshot3")

                .verifyObservedDeleteCount(0)
                .verifyGetCountReturnsExpectedCount(3)
                .verifyGetDeviceReturnsNonNull("device-002")
                .verifyGetDeviceReturnsNonNull("device-003")

                .deviceEventSourceSnapshot("snapshot1")

                .verifyObservedDeleteCount(2)
                .verifyGetCountReturnsExpectedCount(1)
                .verifyGetDeviceReturnsNull("device-002")
                .verifyGetDeviceReturnsNull("device-003")
        ;

        // test deletes emitted in response to DeviceCollection.removeDevice calls
        newDeviceCollectionTester()
                .deviceCollectionStart()

                .deviceCollectionObserveDeletes()

                .deviceEventSourceSnapshot()
                .deviceCollectionRemoveDevice("device-001")

                .verifyObservedDeleteCount(1)
                .verifyGetCountReturnsExpectedCount(0)
                .verifyGetDeviceReturnsNull("device-001")
                ;
    }

    @Test
    public void getCount() throws Exception {
        newDeviceCollectionTester()
                // getCount should throw if called before start
                .deviceCollectionGetCountWithCatch()
                .verifyThrownIllegalStateException()
                ;

        newDeviceCollectionTester()
                .deviceCollectionStart()
                .verifyGetCountReturnsExpectedCount(0)

                .deviceCollectionAddDevice()
                .verifyGetCountReturnsExpectedCount(1)

                // getCount should throw is called after stop
                .deviceCollectionStop()
                .deviceCollectionGetCountWithCatch()
                .verifyThrownIllegalStateException()
                ;
    }

    @Test
    public void getDevice() throws Exception {
        newDeviceCollectionTester()
                // getDevice should throw if called before start
                .deviceCollectionGetDevicesWithCatch()
                .verifyThrownIllegalStateException()
                ;

        newDeviceCollectionTester()
                .deviceCollectionStart()

                .verifyGetDeviceReturnsNull("bogus")

                .deviceCollectionAddDevice()

                .verifyGetDeviceReturnsNonNull("device-id")
                .verifyGetDeviceReturnsNull("bogus")
                ;
    }


    private DeviceCollectionTester newDeviceCollectionTester() {
        return new DeviceCollectionTester();
    }

    private static class DeviceCollectionTester {
        final ResourceLoader resourceLoader = new ResourceLoader("resources/deviceCollection/");
        final MockDeviceEventSource deviceEventSource = new MockDeviceEventSource();
        final MockAferoClient aferoClient = new MockAferoClient();
        final DeviceCollection deviceCollection;

        Throwable thrown;
        DeviceModel deviceModelReturnedFromAddDevice;

        RecordObserver<DeviceModel> createObserver = new RecordObserver<>();
        RecordObserver<DeviceModel> deleteObserver = new RecordObserver<>();
        RecordObserver<DeviceCollection> snapshotObserver = new RecordObserver<>();
        RecordObserver<DeviceModel> profileChangeObserver = new RecordObserver<>();

        DeviceCollectionTester() {
            deviceCollection = new DeviceCollection(deviceEventSource, aferoClient);
        }

        DeviceCollectionTester deviceCollectionStart() {
            deviceCollection.start();
            return this;
        }

        DeviceCollectionTester deviceCollectionStartWithCatch() {
            try {
                deviceCollection.start();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionStop() {
            deviceCollection.stop();
            return this;
        }

        DeviceCollectionTester deviceCollectionStopWithCatch() {
            try {
                deviceCollection.stop();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionAddDevice() {
            deviceCollection.addDevice("test-association-id", false)
                    .subscribe(new Action1<DeviceModel>() {
                        @Override
                        public void call(DeviceModel deviceModel) {
                            deviceModelReturnedFromAddDevice = deviceModel;
                        }
                    });
            return this;
        }

        DeviceCollectionTester deviceCollectionAddDeviceWithCatch() {
            try {
                deviceCollection.addDevice("foo", false).subscribe();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionRemoveDevice(String deviceId) {
            DeviceModel deviceModel = deviceCollection.getDevice(deviceId);
            deviceCollection.removeDevice(deviceModel).subscribe();
            return this;
        }

        DeviceCollectionTester deviceCollectionRemoveDevice() {
            deviceCollection.removeDevice(null).subscribe();
            return this;
        }

        DeviceCollectionTester deviceCollectionRemoveDeviceWithCatch() {
            try {
                deviceCollection.removeDevice(null).subscribe();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionGetCount() {
            deviceCollection.getCount();
            return this;
        }

        DeviceCollectionTester deviceCollectionGetCountWithCatch() {
            try {
                deviceCollection.getCount();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionGetDevice() {
            deviceCollection.getDevice("bogus");
            return this;
        }

        DeviceCollectionTester deviceCollectionGetDevices() {
            deviceCollection.getDevices().subscribe();
            return this;
        }

        DeviceCollectionTester deviceCollectionGetDevicesWithCatch() {
            try {
                deviceCollection.getDevices().subscribe();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionReset() {
            deviceCollection.reset();
            return this;
        }

        DeviceCollectionTester deviceCollectionResetWithCatch() {
            try {
                deviceCollection.reset();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveCreates() {
            deviceCollection.observeCreates()
                .subscribe(createObserver);
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveCreatesWithCatch() {
            try {
                deviceCollection.observeCreates()
                        .subscribe(createObserver);
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveDeletes() {
            deviceCollection.observeDeletes()
                .subscribe(deleteObserver);
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveDeletesWithCatch() {
            try {
                deviceCollection.observeDeletes()
                        .subscribe(deleteObserver);
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveSnapshots() {
            deviceCollection.observeSnapshots()
                .subscribe(snapshotObserver);
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveSnapshotsWithCatch() {
            try {
                deviceCollection.observeSnapshots()
                        .subscribe(snapshotObserver);
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveProfileChanges() {
            deviceCollection.observeProfileChanges()
                .subscribe(profileChangeObserver);
            return this;
        }

        DeviceCollectionTester deviceCollectionObserveProfileChangesWithCatch() {
            try {
                deviceCollection.observeProfileChanges()
                        .subscribe(profileChangeObserver);
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        DeviceCollectionTester deviceEventSourceSnapshot() throws IOException {
            return deviceEventSourceSnapshot("snapshot1");
        }

        DeviceCollectionTester deviceEventSourceSnapshot(String snapshotName) throws IOException {
            DeviceSync[] snapshot = resourceLoader.createObjectFromJSONResource(snapshotName + ".json", DeviceSync[].class);
            deviceEventSource.putSnapshot(snapshot);
            return this;
        }

        DeviceCollectionTester deviceEventSourceProfileChange() throws IOException {
            InvalidateMessage invalidate = resourceLoader.createObjectFromJSONResource("invalidateMessage.json", InvalidateMessage.class);
            deviceEventSource.putInvalidateMessage(invalidate);
            return this;
        }


        DeviceCollectionTester verifyDeviceEventSourceHasOneObserver() {
            assertTrue(deviceEventSource.mSnapshotSubject.hasObservers());
            assertEquals(1, deviceEventSource.mSnapshotSubscriptionCount);
            assertTrue(deviceEventSource.mAttributeChangeSubject.hasObservers());
            assertEquals(1, deviceEventSource.mAttributeChangeSubscriptionCount);
            assertTrue(deviceEventSource.mDeviceErrorSubject.hasObservers());
            assertEquals(1, deviceEventSource.mDeviceErrorSubscriptionCount);
            assertTrue(deviceEventSource.mDeviceMuteSubject.hasObservers());
            assertEquals(1, deviceEventSource.mDeviceMuteSubscriptionCount);
            assertTrue(deviceEventSource.mDeviceStateSubject.hasObservers());
            assertEquals(1, deviceEventSource.mDeviceStateSubscriptionCount);
            assertTrue(deviceEventSource.mInvalidateMessageSubject.hasObservers());
            assertEquals(1, deviceEventSource.mInvalidateMessageSubscriptionCount);
            assertTrue(deviceEventSource.mOTAInfoSubject.hasObservers());
            assertEquals(1, deviceEventSource.mOTAInfoSubscriptionCount);
            return this;
        }

        DeviceCollectionTester verifyDeviceEventSourceHasNoObservers() {
            assertFalse(deviceEventSource.mSnapshotSubject.hasObservers());
            return this;
        }

        DeviceCollectionTester verifyThrownIllegalStateException() {
            assertNotNull(thrown);
            assertTrue(thrown instanceof IllegalStateException);
            return this;
        }

        DeviceCollectionTester verifyAddDeviceReturnedDeviceModel() {
            assertNotNull(deviceModelReturnedFromAddDevice);
            return this;
        }

        DeviceCollectionTester verifyGetCountReturnsExpectedCount(int expectedCount) {
            assertEquals(expectedCount, deviceCollection.getCount());
            return this;
        }

        DeviceCollectionTester verifyGetDeviceReturnsNonNull(String deviceId) {
            assertNotNull(deviceCollection.getDevice(deviceId));
            return this;
        }

        DeviceCollectionTester verifyGetDeviceReturnsNull(String deviceId) {
            assertNull(deviceCollection.getDevice(deviceId));
            return this;
        }

        DeviceCollectionTester verifyObservedCreateCount(int expectedCount) {
            assertEquals(expectedCount, createObserver.onNextList.size());
            return this;
        }

        DeviceCollectionTester verifyObservedSnapshotCount(int expectedCount) {
            assertEquals(expectedCount, snapshotObserver.onNextList.size());
            return this;
        }

        DeviceCollectionTester verifyObservedProfileChangeCount(int expectedCount) {
            assertEquals(expectedCount, profileChangeObserver.onNextList.size());
            return this;
        }

        DeviceCollectionTester verifyObservedDeleteCount(int expectedCount) {
            assertEquals(expectedCount, deleteObserver.onNextList.size());
            return this;
        }

        class RecordObserver<T> implements Observer<T> {

            final Vector<T> onNextList = new Vector<>();

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(T t) {
                onNextList.add(t);
            }
        }

    }

    private static class MockAferoClient implements AferoClient {

        private final ResourceLoader mLoader = new ResourceLoader("resources/deviceCollection/");

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
        public Observable<DeviceProfile> getDeviceProfile(String profileId) {
            try {
                return Observable.just(mLoader.createObjectFromJSONResource(
                        "getDeviceProfile/" + profileId + ".json",
                        DeviceProfile.class));
            } catch (IOException e) {
                return Observable.error(e);
            }
        }

        @Override
        public Observable<DeviceProfile[]> getAccountDeviceProfiles() {
            try {
                return Observable.just(mLoader.createObjectFromJSONResource(
                        "getAccountDeviceProfiles.json",
                        DeviceProfile[].class));
            } catch (IOException e) {
                return Observable.error(e);
            }
        }

        @Override
        public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId) {
            return null;
        }

        @Override
        public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
            return null;
        }

        @Override
        public Observable<DeviceAssociateResponse> deviceAssociateGetProfile(String associationId, boolean isOwnershipVerified) {
            try {
                return Observable.just(mLoader.createObjectFromJSONResource(
                        "deviceAssociateGetProfile.json",
                        DeviceAssociateResponse.class));
            } catch (IOException e) {
                return Observable.error(e);
            }
        }

        @Override
        public Observable<DeviceAssociateResponse> deviceAssociate(String associationId) {
            return null;
        }

        @Override
        public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
            return Observable.just(deviceModel);
        }

        @Override
        public Observable<Void> putDeviceTimezone(DeviceModel deviceModel, TimeZone tz) {
            return Observable.just(null);
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
}