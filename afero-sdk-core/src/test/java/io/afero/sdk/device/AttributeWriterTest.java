/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceSync;
import rx.Observable;
import rx.Observer;
import rx.functions.Action0;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class AttributeWriterTest {

    @Test
    public void testAPIError() throws Exception {
        final int ATTR_ID = 100;

        makeTester()
                .putAttribute(ATTR_ID, "1")
                .commit()

                .deviceUpdate(ATTR_ID, 1, "1")

                .verifyNoError()
                .verifyIsCompleted()
                .verifyResultStatus(ATTR_ID, AttributeWriter.Result.Status.SUCCESS)
        ;
    }

    @Test
    public void testCommitWithoutAttributes() throws Exception {
        makeTester()
                .verifyCommitException()
        ;
    }

    @Test
    public void testSingleAttribute() throws Exception {
        final int ATTR_ID = 100;

        makeTester()
                .putAttribute(ATTR_ID, "1")
                .commit()

                .deviceUpdate(ATTR_ID, 1, "1")

                .verifyNoError()
                .verifyIsCompleted()
                .verifyResultStatus(ATTR_ID, AttributeWriter.Result.Status.SUCCESS)
        ;
    }

    @Test
    public void testTimeout() throws Exception {
        final int ATTR_ID = 100;

        makeTester(1)
                .putAttribute(ATTR_ID, "1")
                .commit()

                .waitFor(2)

                .verifyTimeout()
                .verifyResultStatus(ATTR_ID, AttributeWriter.Result.Status.TIMEOUT)
        ;
    }

    @Test
    public void testMultipleAttributes() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")
                .commit()

                .deviceUpdate(ATTR_ID_1, 1, "1")
                .deviceUpdate(ATTR_ID_2, 2, "2")
                .deviceUpdate(ATTR_ID_3, 3, "3")

                .verifyNoError()
                .verifyIsCompleted()
                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.SUCCESS)
        ;
    }

    @Test
    public void testMultipleAttributesWithRequestFailures() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")

                .addAttributeWriteResponse(1, WriteResponse.STATUS_SUCCESS)
                .addAttributeWriteResponse(2, WriteResponse.STATUS_FAILURE)
                .addAttributeWriteResponse(3, WriteResponse.STATUS_NOT_ATTEMPTED)

                .commit()

                .deviceUpdate(ATTR_ID_1, 1, "1")

                .verifyAttributeWriteFailure()

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.FAILURE)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.NOT_ATTEMPTED)
        ;
    }

    @Test
    public void testMultipleAttributesWithAllRequestFailures() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")

                .addAttributeWriteResponse(1, WriteResponse.STATUS_FAILURE)
                .addAttributeWriteResponse(2, WriteResponse.STATUS_NOT_ATTEMPTED)
                .addAttributeWriteResponse(3, WriteResponse.STATUS_NOT_ATTEMPTED)

                .commit()

                .verifyAttributeWriteFailure()

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.FAILURE)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.NOT_ATTEMPTED)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.NOT_ATTEMPTED)
        ;
    }

    @Test
    public void testManyBatchedAttributes() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;
        final int ATTR_ID_4 = 400;
        final int ATTR_ID_5 = 500;
        final int ATTR_ID_6 = 600;
        final int ATTR_ID_7 = 700;
        final int ATTR_ID_8 = 800;
        final int ATTR_ID_9 = 900;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")
                .putAttribute(ATTR_ID_4, "4")
                .putAttribute(ATTR_ID_5, "5")
                .putAttribute(ATTR_ID_6, "6")
                .putAttribute(ATTR_ID_7, "7")
                .putAttribute(ATTR_ID_8, "8")
                .putAttribute(ATTR_ID_9, "9")

                .commit()

                .deviceUpdate(ATTR_ID_1, 1, "1")
                .deviceUpdate(ATTR_ID_2, 2, "2")
                .deviceUpdate(ATTR_ID_3, 3, "3")
                .deviceUpdate(ATTR_ID_4, 4, "4")
                .deviceUpdate(ATTR_ID_5, 5, "5")
                .deviceUpdate(ATTR_ID_6, 6, "6")
                .deviceUpdate(ATTR_ID_7, 7, "7")
                .deviceUpdate(ATTR_ID_8, 8, "8")
                .deviceUpdate(ATTR_ID_9, 9, "9")

                .verifyNoError()
                .verifyIsCompleted()

                .verifyMultipleRequestsWereExecuted()

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_4, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_5, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_6, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_7, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_8, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_9, AttributeWriter.Result.Status.SUCCESS)
        ;
    }

    @Test
    public void testManyBatchedAttributesWithRequestFailures() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;
        final int ATTR_ID_4 = 400;
        final int ATTR_ID_5 = 500;
        final int ATTR_ID_6 = 600;
        final int ATTR_ID_7 = 700;
        final int ATTR_ID_8 = 800;
        final int ATTR_ID_9 = 900;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")
                .putAttribute(ATTR_ID_4, "4")
                .putAttribute(ATTR_ID_5, "5")
                .putAttribute(ATTR_ID_6, "6")
                .putAttribute(ATTR_ID_7, "7")
                .putAttribute(ATTR_ID_8, "8")
                .putAttribute(ATTR_ID_9, "9")

                .addAttributeWriteResponse(1, WriteResponse.STATUS_SUCCESS)
                .addAttributeWriteResponse(2, WriteResponse.STATUS_SUCCESS)
                .addAttributeWriteResponse(3, WriteResponse.STATUS_SUCCESS)
                .addAttributeWriteResponse(4, WriteResponse.STATUS_FAILURE)
                .addAttributeWriteResponse(5, WriteResponse.STATUS_NOT_ATTEMPTED)
                .addAttributeWriteResponse(6, WriteResponse.STATUS_NOT_ATTEMPTED)
                .addAttributeWriteResponse(7, WriteResponse.STATUS_NOT_ATTEMPTED)
                .addAttributeWriteResponse(8, WriteResponse.STATUS_NOT_ATTEMPTED)
                .addAttributeWriteResponse(9, WriteResponse.STATUS_NOT_ATTEMPTED)

                .commit()

                .deviceUpdate(ATTR_ID_1, 1, "1")
                .deviceUpdate(ATTR_ID_2, 2, "2")
                .deviceUpdate(ATTR_ID_3, 3, "3")

                .verifyAttributeWriteFailure()
                .verifySingleRequestWasExecuted()

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_4, AttributeWriter.Result.Status.FAILURE)
                .verifyResultStatus(ATTR_ID_5, AttributeWriter.Result.Status.NOT_ATTEMPTED)
                .verifyResultStatus(ATTR_ID_6, AttributeWriter.Result.Status.NOT_ATTEMPTED)
                .verifyResultStatus(ATTR_ID_7, AttributeWriter.Result.Status.NOT_ATTEMPTED)
                .verifyResultStatus(ATTR_ID_8, AttributeWriter.Result.Status.NOT_ATTEMPTED)
                .verifyResultStatus(ATTR_ID_9, AttributeWriter.Result.Status.NOT_ATTEMPTED)
        ;
    }

    @Test
    public void testMultipleAttributesWithHubErrors() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")
                .commit()

                .deviceUpdate(ATTR_ID_1, 1, "1")
                .deviceError(2)
                .deviceError(3)

                .verifyNoError()
                .verifyIsCompleted()

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.FAILURE)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.FAILURE)
        ;
    }


    private Tester makeTester() throws IOException {
        return new Tester();
    }

    private Tester makeTester(int timeoutSeconds) throws IOException {
        return new Tester(timeoutSeconds);
    }

    private static class Tester {
        final TestMockAferoClient aferoClient = new TestMockAferoClient();
        final ResourceLoader resourceLoader = new ResourceLoader("resources/writeAttributeOperation/");
        final DeviceProfile deviceProfile = resourceLoader.createObjectFromJSONResource("deviceProfile.json", DeviceProfile.class);
        final DeviceModel deviceModel = new DeviceModel("device-id", deviceProfile, false, aferoClient);
        final AttributeWriter attributeWriter;
        final Observer<AttributeWriter.Result> attributeWriterObserver = new AttributeWriterTestObserver();
        ArrayList<WriteResponse> postBatchAttributeWriteResponses;

        HashMap<Integer, AttributeWriter.Result> writeResults = new HashMap<>();
        Throwable error;
        boolean isCompleted;

        Tester() throws IOException {
            attributeWriter = new AttributeWriter(deviceModel);
        }

        Tester(int timeoutSeconds) throws IOException {
            attributeWriter = new AttributeWriter(deviceModel, timeoutSeconds);
        }

        Tester putAttribute(int attrId, String value) {
            DeviceProfile.Attribute attribute = deviceModel.getAttributeById(attrId);
            attributeWriter.put(attrId, new AttributeValue(value, attribute.getDataType()));
            return this;
        }

        Tester commit() {
            if (postBatchAttributeWriteResponses != null) {
                WriteResponse[] rr = new WriteResponse[postBatchAttributeWriteResponses.size()];
                postBatchAttributeWriteResponses.toArray(rr);
                aferoClient.setPostBatchAttributeWriteResponse(Observable.just(rr));
            }

            attributeWriter.commit().subscribe(attributeWriterObserver);
            return this;
        }

        Tester addAttributeWriteResponse(int reqId, String requestResponseStatus) {

            if (postBatchAttributeWriteResponses == null) {
                postBatchAttributeWriteResponses = new ArrayList<>();
            }

            WriteResponse wr = new WriteResponse();
            wr.requestId = reqId;
            wr.status = requestResponseStatus;
            wr.timestampMs = System.currentTimeMillis();

            postBatchAttributeWriteResponses.add(wr);

            return this;
        }

        Tester waitFor(int seconds) {
            Observable.empty()
                .delay(seconds, TimeUnit.SECONDS)
                .toBlocking()
                .subscribe();

            return this;
        }

        Tester verifyIsCompleted() {
            assertTrue(isCompleted);
            return this;
        }

        Tester verifyNoError() {
            assertNull(error);
            return this;
        }

        Tester verifyResultStatus(int attrId, AttributeWriter.Result.Status status) {
            assertNotNull(writeResults.get(attrId));
            assertEquals(status, writeResults.get(attrId).status);
            return this;
        }

        Tester deviceUpdate(int attr_id, int reqId, String value) {
            DeviceSync ds = new DeviceSync();
            ds.setDeviceId(deviceModel.getId());
            ds.requestId = reqId;
            ds.attribute = new DeviceSync.AttributeEntry(attr_id, value);

            deviceModel.update(ds);

            return this;
        }

        Tester deviceError(int reqId) {
            DeviceError error = new DeviceError();
            error.requestId = reqId;
            error.status = "Status::FAIL";

            deviceModel.onError(error);

            return this;
        }

        Tester verifyCommitException() {
            boolean caughtIllegalArgumentException = false;
            try {
                attributeWriter.commit().subscribe(attributeWriterObserver);
            } catch (Exception e) {
                caughtIllegalArgumentException = e instanceof IllegalArgumentException;
            }
            assertTrue(caughtIllegalArgumentException);
            return this;
        }

        Tester verifyAttributeWriteFailure() {
            assertNotNull(error);
            assertTrue(error instanceof AttributeWriter.AttributeWriteRequestFailure);
            return this;
        }

        Tester verifyMultipleRequestsWereExecuted() {
            assertTrue(aferoClient.requestCount_postBatchAttributeWrite > 1);
            return this;
        }

        Tester verifySingleRequestWasExecuted() {
            assertEquals(1, aferoClient.requestCount_postBatchAttributeWrite);
            return this;
        }

        Tester verifyTimeout() {
            assertNotNull(error);
            assertTrue(error instanceof TimeoutException);
            return this;
        }

        private class AttributeWriterTestObserver implements Observer<AttributeWriter.Result> {

            @Override
            public void onCompleted() {
                isCompleted = true;
            }

            @Override
            public void onError(Throwable e) {
                error = e;
//                e.printStackTrace();
            }

            @Override
            public void onNext(AttributeWriter.Result result) {
                writeResults.put(result.attributeId, result);
            }
        }
    }

    private static class TestMockAferoClient extends MockAferoClient {
        private int requestCount_postBatchAttributeWrite;

        @Override
        public Observable<WriteResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, WriteRequest[] body, int maxRetryCount, int statusCode) {
            return super.postBatchAttributeWrite(deviceModel, body, maxRetryCount, statusCode)
                    .doOnSubscribe(new Action0() {
                        @Override
                        public void call() {
                            requestCount_postBatchAttributeWrite++;
                        }
                    });
        }
    }

}