package io.afero.sdk.device;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceSync;
import rx.Observable;
import rx.Observer;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;


public class AttributeWriterTest {

    @Test
    public void testAPIError() throws Exception {
        final int ATTR_ID = 100;

        makeTester()
                .putAttribute(ATTR_ID, "1")
                .commit()
                .deviceUpdate(ATTR_ID, 1, "1")
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
                .verifyResultStatus(ATTR_ID, AttributeWriter.Result.Status.SUCCESS)
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

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.SUCCESS)
        ;
    }

    @Test
    public void testMultipleAttributesWithRequestAPIErrors() throws Exception {
        final int ATTR_ID_1 = 100;
        final int ATTR_ID_2 = 200;
        final int ATTR_ID_3 = 300;

        makeTester()
                .putAttribute(ATTR_ID_1, "1")
                .putAttribute(ATTR_ID_2, "2")
                .putAttribute(ATTR_ID_3, "3")

                .commitWithRequestAPIErrors()

                .deviceUpdate(ATTR_ID_1, 1, "1")

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.FAILURE)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.FAILURE)
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

                .verifyResultStatus(ATTR_ID_1, AttributeWriter.Result.Status.SUCCESS)
                .verifyResultStatus(ATTR_ID_2, AttributeWriter.Result.Status.FAILURE)
                .verifyResultStatus(ATTR_ID_3, AttributeWriter.Result.Status.FAILURE)
        ;
    }


    Tester makeTester() throws IOException {
        return new Tester();
    }

    private static class Tester {
        final MockAferoClient aferoClient = new MockAferoClient();
        final ResourceLoader resourceLoader = new ResourceLoader("resources/writeAttributeOperation/");
        final DeviceProfile deviceProfile = resourceLoader.createObjectFromJSONResource("deviceProfile.json", DeviceProfile.class);
        final DeviceModel deviceModel = new DeviceModel("device-id", deviceProfile, false, aferoClient);
        final AttributeWriter wao = new AttributeWriter(deviceModel);
        final Observer<AttributeWriter.Result> waoObserver = new WAOTestObserver();

        HashMap<Integer, AttributeWriter.Result> writeResults = new HashMap<>();
        Throwable error;
        boolean isCompleted;

        Tester() throws IOException {
        }

        Tester putAttribute(int attrId, String value) {
            DeviceProfile.Attribute attribute = deviceModel.getAttributeById(attrId);
            wao.put(attrId, new AttributeValue(value, attribute.getDataType()));
            return this;
        }

        Tester commit() {
            wao.commit().subscribe(waoObserver);
            return this;
        }

        Tester commitWithRequestAPIErrors() {

            RequestResponse[] response = new RequestResponse[3];
            RequestResponse rr = new RequestResponse();
            rr.requestId = 1;
            rr.status = RequestResponse.STATUS_SUCCESS;
            rr.timestampMs = System.currentTimeMillis();
            response[0] = rr;

            rr = new RequestResponse();
            rr.status = RequestResponse.STATUS_FAILURE;
            response[1] = rr;

            rr = new RequestResponse();
            rr.status = RequestResponse.STATUS_NOT_ATTEMPTED;
            response[2] = rr;

            aferoClient.setPostBatchAttributeWriteResponse(Observable.just(response));

            wao.commit().subscribe(waoObserver);

            return this;
        }

        Tester verifyResultStatus(int attrId, AttributeWriter.Result.Status status) {
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
                wao.commit().subscribe(waoObserver);
            } catch (Exception e) {
                caughtIllegalArgumentException = e instanceof IllegalArgumentException;
            }
            assertTrue(caughtIllegalArgumentException);
            return this;
        }

        private class WAOTestObserver implements Observer<AttributeWriter.Result> {

            @Override
            public void onCompleted() {
                isCompleted = true;
            }

            @Override
            public void onError(Throwable e) {
                error = e;
            }

            @Override
            public void onNext(AttributeWriter.Result result) {
                writeResults.put(result.attributeId, result);
            }
        }
    }

}