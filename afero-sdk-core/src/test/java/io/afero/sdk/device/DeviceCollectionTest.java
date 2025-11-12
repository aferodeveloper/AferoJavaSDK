/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Test;

import java.io.IOException;
import java.util.Vector;

import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.InvalidateMessage;
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
                .deviceCollectionStart()
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
                .verifyGetCountReturnsExpectedCount(2)
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
                .deviceCollectionStart()
                .deviceCollectionAddDevice()

                .verifyAddDeviceReturnedDeviceModel()
                .verifyGetCountReturnsExpectedCount(2)
                .verifyGetDeviceReturnsNonNull("device-id")
        ;
    }

    @Test
    public void reset() throws Exception {
        newDeviceCollectionTester()
                .deviceCollectionStart()
                .deviceCollectionAddDevice()

                .verifyGetCountReturnsExpectedCount(2)

                .deviceCollectionReset()

                .verifyGetCountReturnsExpectedCount(0)
                ;
    }

    @Test
    public void observeCreates() throws Exception {
        // test creates emitted in response to snapshots
        newDeviceCollectionTester()
                .deviceCollectionStart()

                .deviceCollectionObserveCreates()
                .verifyGetCountReturnsExpectedCount(1)
                .verifyGetDeviceReturnsNonNull("device-001")

                .deviceEventSourceSnapshot("snapshot3")

                .verifyObservedCreateCount(2)
                .verifyGetCountReturnsExpectedCount(3)

                .verifyGetDeviceReturnsNonNull("device-001")
                .verifyDeviceHasProfile("device-001", "profile-001")

                .verifyGetDeviceReturnsNonNull("device-002")
                .verifyDeviceHasProfile("device-002", "profile-002")

                .verifyGetDeviceReturnsNonNull("device-003")
                .verifyDeviceHasProfile("device-003", "profile-003")

        ;

        // TODO investigate why this fails

        // test creates emitted in response to snapshots, with a failed getProfile
//        newDeviceCollectionTester()
//                .deviceCollectionStart()
//
//                .deviceCollectionObserveCreates()
//                .verifyGetCountReturnsExpectedCount(1)
//                .verifyGetDeviceReturnsNonNull("device-001")
//
//                .deviceEventSourceSnapshot("snapshot4")
//
//                .verifyObservedCreateCount(1)
//                .verifyGetCountReturnsExpectedCount(2)
//
//                .verifyGetDeviceReturnsNonNull("device-001")
//                .verifyDeviceHasProfile("device-001", "profile-001")
//
//                .verifyGetDeviceReturnsNull("device-002")
//
//                .verifyGetDeviceReturnsNonNull("device-003")
//                .verifyDeviceHasProfile("device-003", "profile-003")
//
//        ;

        newDeviceCollectionTester()
                .deviceCollectionStartWithNoDevices()
                .deviceCollectionObserveCreates()

                .deviceEventSourceSnapshot()

                .verifyObservedCreateCount(1)
        ;
    }

    @Test
    public void observeProfileChanges() throws Exception {
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
                .deviceCollectionStart()
                .deviceCollectionObserveSnapshots()

                .deviceEventSourceSnapshot()

                .verifyObservedSnapshotCount(1)
        ;
    }

    @Test
    public void observeDeletes() throws Exception {
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
                .deviceCollectionStart()
                .verifyGetCountReturnsExpectedCount(1)

                .deviceCollectionAddDevice()
                .verifyGetCountReturnsExpectedCount(2)
                ;
    }

    @Test
    public void getDevice() throws Exception {
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
        static final String PATH_PREFIX = "deviceCollection/";
        final ResourceLoader resourceLoader = new ResourceLoader(PATH_PREFIX);
        final MockAferoClient aferoClient = new MockAferoClient(PATH_PREFIX);
        final MockDeviceEventSource deviceEventSource = new MockDeviceEventSource();
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
            deviceCollection.start().subscribe(new Observer<DeviceCollection>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    thrown = e;
                }

                @Override
                public void onNext(DeviceCollection deviceCollection) {

                }
            });
            return this;
        }

        DeviceCollectionTester deviceCollectionStartWithNoDevices() {
            aferoClient.setFileGetDevices("getDevicesEmpty.json");
            deviceCollection.start().subscribe(new Observer<DeviceCollection>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    thrown = e;
                }

                @Override
                public void onNext(DeviceCollection deviceCollection) {

                }
            });

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
            deviceCollection.addDevice("genericDevice", false)
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

        DeviceCollectionTester verifyDeviceHasProfile(String deviceId, String profileId) {
            DeviceModel deviceModel = deviceCollection.getDevice(deviceId);
            assertNotNull(deviceModel);
            assertNotNull(deviceModel.getProfile());
            assertEquals(profileId, deviceModel.getProfile().getId());
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
}