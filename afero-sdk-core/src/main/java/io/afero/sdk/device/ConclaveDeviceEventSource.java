/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;

import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.conclave.ConclaveAccessManager;
import io.afero.sdk.conclave.ConclaveClient;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.DeviceEventSource;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceMute;
import io.afero.sdk.conclave.models.DeviceState;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.InvalidateMessage;
import io.afero.sdk.conclave.models.OTAInfo;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class ConclaveDeviceEventSource implements DeviceEventSource {

    private ConclaveClient mConclaveClient = new ConclaveClient();
    private ConclaveAccessManager mConclaveAccessManager;

    private PublishSubject<DeviceSync[]> mSnapshotSubject = PublishSubject.create();
    private PublishSubject<DeviceSync> mAttributeChangeSubject = PublishSubject.create();
    private PublishSubject<DeviceError> mDeviceErrorSubject = PublishSubject.create();
    private PublishSubject<DeviceState> mStatusChange = PublishSubject.create();
    private PublishSubject<DeviceMute> mDeviceMuteSubject = PublishSubject.create();
    private PublishSubject<OTAInfo> mOTASubject = PublishSubject.create();
    private PublishSubject<InvalidateMessage> mInvalidateSubject = PublishSubject.create();

    private static final String KEY_EVENT = "event";
    private static final String KEY_DATA = "data";

    private String mAccountId;
    private String mUserId;
    private String mToken;
    private String mType;
    private boolean mSessionTrace;

    private ConclaveAccessDetails mConclaveAccessDetails;

    private long mGeneration;
    private int mSequenceNum;

    private Subscription mConclaveSubscription;

    private Observer<JsonNode> mConclaveObserver = new Observer<JsonNode>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            // should never get here
            AfLog.e("ConclaveDeviceEventSource Conclave Observer error!");
            AfLog.e(e);
        }

        @Override
        public void onNext(JsonNode node) {
            try {
                onNextConclave(node);
            } catch (Exception e) {
                // eat all exceptions - the spice must flow
                AfLog.e(e);
            }
        }
    };

    public ConclaveDeviceEventSource(ConclaveAccessManager cam) {
        mConclaveAccessManager = cam;

        cam.getObservable().subscribe(mConclaveAccessObserver);
    }

    public Observable<ConclaveClient.Status> observeConclaveStatus() {
        return mConclaveClient.statusObservable();
    }

    public rx.Observable<ConclaveDeviceEventSource> start(String accountId, String userId, String type) {
        mAccountId = accountId;
        mUserId = userId;
        mType = type;
        mGeneration = 0;
        mSequenceNum = 0;

        return reconnect();
    }

    public boolean hasStarted() {
        return mAccountId != null;
    }

    public void setAccountId(String accountId) {
        mAccountId = accountId;
        mConclaveAccessManager.resetAccess();
        mConclaveAccessDetails = null;
    }

    public void setSessionTracing(boolean enabled) {
        mSessionTrace = enabled;
    }

    public boolean isSessionTracingEnabled() {
        return mSessionTrace;
    }

    public void viewDevice(String deviceId, boolean isViewing) {
        mConclaveClient.sayAsync("device:view", new ConclaveMessage.ViewDeviceFields(deviceId, isViewing))
            .subscribe(new RxUtils.IgnoreResponseObserver<ConclaveMessage.Say>());
    }

    public void sendMetrics(ConclaveMessage.Metric metric) {
        mConclaveClient.sayAsync("metrics", metric)
            .subscribe(new RxUtils.IgnoreResponseObserver<ConclaveMessage.Say>());
    }

    public void resetSequence() {
        mGeneration = 0;
        mSequenceNum = 0;
    }

    public void stop() {

        if (mConclaveSubscription != null) {
            mConclaveSubscription.unsubscribe();
            mConclaveSubscription = null;
        }

        mConclaveClient.close();
    }

    public rx.Observable<ConclaveDeviceEventSource> reconnect() {

        if (mConclaveSubscription != null) {
            mConclaveSubscription.unsubscribe();
        }

        mConclaveSubscription = mConclaveClient.messageObservable()
            .subscribe(mConclaveObserver);

        rx.Observable<ConclaveDeviceEventSource> connectObservable;

        if (mConclaveAccessDetails == null) {
            connectObservable = mConclaveAccessManager.getAccess()
                .flatMap(new Func1<ConclaveAccessDetails, Observable<ConclaveDeviceEventSource>>() {
                    @Override
                    public Observable<ConclaveDeviceEventSource> call(ConclaveAccessDetails conclaveAccessDetails) {
                        mToken = null;

                        setConclaveAccessDetails(conclaveAccessDetails);

                        if (mToken == null) {
                            return rx.Observable.error(new Exception("ConclaveDeviceEventSource: couldn't find suitable Conclave token"));
                        }

                        AfLog.i("ConclaveDeviceEventSource.reconnect: mAccountId = " + mAccountId);

                        return mConclaveClient.connect(conclaveAccessDetails)
                            .map(new RxUtils.Mapper<ConclaveClient, ConclaveDeviceEventSource>(ConclaveDeviceEventSource.this));
                    }
                });
        } else {
            connectObservable = mConclaveClient.connect(mConclaveAccessDetails)
                .map(new RxUtils.Mapper<ConclaveClient, ConclaveDeviceEventSource>(this));
        }

        return connectObservable;
    }

    public boolean isConnected() {
        return mConclaveSubscription != null && mConclaveClient.isConnected();
    }

    @Override
    public Observable<DeviceSync[]> observeSnapshot() {
        return mSnapshotSubject;
    }

    @Override
    public Observable<DeviceSync> observeAttributeChange() {
        return mAttributeChangeSubject;
    }

    @Override
    public Observable<DeviceError> observeError() {
        return mDeviceErrorSubject;
    }

    @Override
    public Observable<DeviceState> observeStatusChange() {
        return mStatusChange;
    }

    @Override
    public Observable<DeviceMute> observeMute() {
        return mDeviceMuteSubject;
    }

    @Override
    public Observable<OTAInfo> observeOTA() {
        return mOTASubject;
    }

    @Override
    public Observable<InvalidateMessage> observeInvalidate() {
        return mInvalidateSubject;
    }

    private void onNextConclave(JsonNode node) {
        Map.Entry<String,JsonNode> entry = node.fields().next();
        String key = entry.getKey().toLowerCase(Locale.ROOT);

        if (key.equals("public") || key.equals("private")) {
            onMessage(entry.getValue());
        }
        else if (key.equals("hello")) {
            mConclaveClient.login(mAccountId, mUserId, mToken, mType, mSessionTrace);
        }
        else if (key.equals("welcome")) {
            long generation = mGeneration;
            JsonNode genNode = entry.getValue().get("generation");
            if (genNode != null) {
                generation = genNode.asLong();
            }

            int seq = mSequenceNum;
            JsonNode seqNode = entry.getValue().get("seq");
            if (seqNode != null) {
                seq = seqNode.asInt();
            }

            if (mGeneration != generation || mSequenceNum != seq) {
                AfLog.i("ConclaveDeviceEventSource: generation/sequence # mismatch " + mGeneration + " != " + generation + " || " + mSequenceNum + " != " + seq);
                mGeneration = generation;
                mSequenceNum = seq;
//                mConclaveClient.say("snapshot?", null);
            } else {
                AfLog.i("ConclaveDeviceEventSource: generation/sequence # match " + mGeneration + "/" + mSequenceNum);
            }
        }
        else if (key.equals("error")) {
            JsonNode errNode = entry.getValue().get("code");
            if (errNode != null) {
                int err = errNode.asInt();
                if (err == ConclaveClient.ERROR_CODE_INVALID_TOKEN) {
                    mConclaveAccessManager.updateAccess();
                }
            }
        }
    }

    private void onMessage(JsonNode node) {
        try {
            String event = node.get(KEY_EVENT).asText().toLowerCase(Locale.ROOT);
            JsonNode data = node.get(KEY_DATA);
            final ObjectMapper mapper = JSONUtils.getObjectMapper();

            int seq = 0;
            JsonNode seqNode = node.get("seq");
            if (seqNode != null) {
                mSequenceNum = seqNode.asInt();
            }

            if (event.equals("attr_change")) {
                DeviceSync deviceSync = mapper.treeToValue(data, DeviceSync.class);
                deviceSync.seq = seq;
                mAttributeChangeSubject.onNext(deviceSync);
            }
            else if (event.equals("peripherallist")) {
                if (data.has("currentSeq")) {
                    seq = data.get("currentSeq").asInt();
                }

                DeviceSync[] deviceSync = mapper.treeToValue(data.get("peripherals"), DeviceSync[].class);
                for (DeviceSync ds : deviceSync) {
                    ds.seq = seq;
                }

                mSnapshotSubject.onNext(deviceSync);
            }
            else if (event.equals("invalidate")) {
                mInvalidateSubject.onNext(new InvalidateMessage(data.get("kind").asText(), data));
            }
            else if (event.equals("status_change")) {
                DeviceState state = mapper.treeToValue(data, DeviceState.class);
                mStatusChange.onNext(state);
            }
            else if (event.equals("device:error")) {
                DeviceError err = mapper.treeToValue(data, DeviceError.class);
                mDeviceErrorSubject.onNext(err);
            }
            else if (event.equals("device:mute")) {
                DeviceMute mute = mapper.treeToValue(data, DeviceMute.class);
                mDeviceMuteSubject.onNext(mute);
            }
            else if (event.equals("device:ota_progress")) {
                OTAInfo otaInfo = mapper.treeToValue(data, OTAInfo.class);
                mOTASubject.onNext(otaInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setConclaveAccessDetails(ConclaveAccessDetails cad) {
        mConclaveAccessDetails = cad;
        mToken = null;

        for (ConclaveAccessDetails.ConclaveAccess ca : cad.tokens) {
            if (ca.client != null) {
                String type = ca.client.get("type");
                if (type != null && type.equalsIgnoreCase("user")) {
                    mToken = ca.token;
                }
            }
        }
    }

    private Observer<ConclaveAccessDetails> mConclaveAccessObserver = new Observer<ConclaveAccessDetails>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onNext(ConclaveAccessDetails cad) {
            setConclaveAccessDetails(cad);
        }
    };

    public void triggerAttributeChange(DeviceModel deviceModel) {
        DeviceSync ds = new DeviceSync();
        ds.setDeviceId(deviceModel.getId());
        mAttributeChangeSubject.onNext(ds);
    }
}
