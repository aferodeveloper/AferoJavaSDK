/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.AferoError;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.DeviceStatus;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.LocationState;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceMute;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.OTAInfo;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.Clock;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.MetricUtil;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;


/**
 * This class manages the attribute state and events associated with a particular instance of an
 * Afero peripheral device.
 */
public class DeviceModel implements ControlModel {

    public static class ApiClientError implements AferoError {
        public int code;
        public String status;
    }

    public enum State {
        NORMAL,
        WAITING_FOR_UPDATE,
        UPDATE_TIMED_OUT
    }

    private enum AvailableState {
        NONE,
        AVAILABLE,
        UNAVAILABLE
    }

    private static final long WRITE_TIMEOUT_INTERVAL = 30000;
    private static final long OTA_WATCHDOG_DELAY = 30L * 1000L;
    private static final int WRITE_ATTRIBUTE_RETRY_COUNT = 4;

    @SuppressWarnings("WeakerAccess")
    private class AttributeData {
        public AttributeValue mCurrentValue;
        public AttributeValue mPendingValue;
        long mExpectedUpdateTime;
    }

    @SuppressWarnings("WeakerAccess")
    public static class AttributeDebug {
        public String pending;
        public String current;
    }

    private static final int HTTP_LOCKED = 423; // https://tools.ietf.org/html/rfc4918#section-11.3

    private Subscription mPendingWriteSubscription;
    private Subscription mOTAWatchdogSubscription;

    private final String mId;
    private final AferoClient mAferoClient;
    private final HashMap<Integer,AttributeData> mAttributes = new HashMap<>();

    private String mName;

    private String mProfileId;
    private DeviceProfile mProfile;
    private int mPendingUpdateCount;

    private AvailableState mAvailableState = AvailableState.NONE;

    private final PublishSubject<DeviceSync> mDeviceSyncUpdateSubject = PublishSubject.create();
    private final PublishSubject<AferoError> mErrorSubject = PublishSubject.create();
    private final PublishSubject<DeviceModel> mProfileUpdateSubject = PublishSubject.create();
    private final PublishSubject<ControlModel> mUpdateSubject = PublishSubject.create();
    private final Observable<ControlModel> mUpdateObservable;

    private boolean mOTAInProgress;
    private int mOTAState;
    private int mOTAProgress;

    private int mRSSI;
    private boolean mIsLinked;
    private boolean mDirect;
    private boolean mIsVirtual;
    private final boolean mIsDeveloperDevice;

    private LocationState mLocationState = new LocationState(LocationState.State.Invalid);

    private AferoError mLastError;

    private OnNextDeviceRequest mOnNextDeviceRequestResponse;
    private OnErrorDeviceRequest mOnErrorDeviceRequestResponse;

    private DeviceModel() {
        mId = null;
        mAferoClient = null;
        mIsDeveloperDevice = false;
        mUpdateObservable = null;
    }

    DeviceModel(String deviceId, DeviceProfile profile, boolean isDeveloperDevice, AferoClient aferoClient) {
        mId = deviceId;
        mAferoClient = aferoClient;
        mIsDeveloperDevice = isDeveloperDevice;
        setProfile(profile);
        mUpdateObservable = mUpdateSubject.onBackpressureBuffer();
    }

    @JsonIgnore
    @Override
    public rx.Observable<ControlModel> getUpdateObservable() {
        return mUpdateObservable;
    }

    @Override
    public boolean enableDisplayRules() {
        return true;
    }

    @Override
    public boolean enableReadOnlyControls() {
        return true;
    }

    @JsonIgnore
    public rx.Observable<DeviceSync> getDeviceSyncObservable() {
        return mDeviceSyncUpdateSubject;
    }

    @JsonIgnore
    public rx.Observable<AferoError> getErrorObservable() {
        return mErrorSubject.onBackpressureBuffer();
    }

    public rx.Observable<DeviceModel> getProfileObservable() {
        return mProfileUpdateSubject;
    }

    @JsonProperty
    public State getState() {
        State state = State.NORMAL;
        long now = Clock.getElapsedMillis();

        for (AttributeData data : mAttributes.values()) {
            if (data.mExpectedUpdateTime != 0) {
                if (now > data.mExpectedUpdateTime) {
                    state = State.UPDATE_TIMED_OUT;
                } else {
                    state = State.WAITING_FOR_UPDATE;
                    break;
                }
            }
        }

        return state;
    }

    public void update(DeviceSync deviceSync) {

        mDeviceSyncUpdateSubject.onNext(deviceSync);

        if (deviceSync.requestId != null && deviceSync.requestId != 0) {
            MetricUtil.getInstance().end(deviceSync.requestId, Clock.getElapsedMillis(), true, null);
        }

        // See https://kibanlabs.atlassian.net/browse/ANDROID-606
        // "For states 0, 1, 4, and 5 your going to want to update the UI with the value that is returned.
        // In each of these cases the device is going to be returning the current value. In the failure"
        // cases this value will not be the value that you attempted to set. It will likely be the
        // previous value of that attribute.
        // For states 2 and 3 the device is going to return 0 length for the value, so we probably
        // don't want to update the UI with that value." --lucas
        boolean hasValidValues = true;
        if (deviceSync.state != null) {
            switch (deviceSync.state) {
                case DeviceSync.UPDATE_STATE_UNKNOWN_UUID:
                case DeviceSync.UPDATE_STATE_LENGTH_EXCEEDED:
                    hasValidValues = false;
                    break;
            }
        }

        boolean hasChanged = false;
        if (hasValidValues && deviceSync.attributes != null) {
            hasChanged = true;
            for (DeviceSync.AttributeEntry ae : deviceSync.attributes) {
                updateAttributeValues(ae.id, ae.value);
            }
        }

        if (hasValidValues && deviceSync.attribute != null) {
            hasChanged = true;
            updateAttributeValues(deviceSync.attribute.id, deviceSync.attribute.value);
        }

        if (deviceSync.profileId != null) {
            hasChanged = true;
            mProfileId = deviceSync.profileId;
        }

        if (deviceSync.friendlyName != null) {
            hasChanged = true;
            mName = deviceSync.friendlyName;
        }

        if (deviceSync.status != null) {
            hasChanged = updateStatus(deviceSync.status) || hasChanged;
        }

        if (deviceSync.virtual != mIsVirtual) {
            hasChanged = true;
            mIsVirtual = deviceSync.virtual;
        }

        if (deviceSync.tags != null) {
            for (DeviceTag tag : deviceSync.tags) {
                putTag(tag.deviceTagId, tag.value);
            }
        }

        if (mPendingUpdateCount > 0) {
            mPendingUpdateCount--;
            hasChanged = true;
        }

        if (hasChanged) {
            mUpdateSubject.onNext(this);
        }
    }

    public boolean isOTAInProgress() {
        return mOTAInProgress;
    }

    public int getOTAState() {
        return mOTAState;
    }

    public int getOTAProgress() {
        return mOTAProgress;
    }

    public void onOTAStop() {
        cancelOTAWatchdog();

        mOTAInProgress = false;
        mOTAState = 0;
        mOTAProgress = 0;

        mUpdateSubject.onNext(this);
    }

    @JsonIgnore
    public AferoError getLastError() {
        return mLastError;
    }

    public void clearLastError() {
        if (mLastError != null) {
            mLastError = null;
            mUpdateSubject.onNext(this);
        }
    }

    @JsonProperty
    public String getId() {
        return mId;
    }

    @JsonIgnore
    public String getName() {

        String name = mName;

        if (name == null || name.isEmpty()) {
            name = mName;
        }

        if (name == null || name.isEmpty()) {
            DeviceProfile.Presentation pp = getPresentation();
            name = pp != null ? pp.getLabel() : null;
        }

        if (name == null || name.isEmpty()) {
            String deviceType = mProfile != null ? mProfile.getDeviceType() : null;
            if (deviceType != null) {
                name = "[" + deviceType + "]";
            }
        }

        if (name == null || name.isEmpty()) {
            name = getProfileID();
        }

        return name != null ? name : "";
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    @JsonIgnore
    public void setAvailable(boolean available) {
        // not used
    }

    @JsonProperty
    public boolean isAvailable() {
        return mAvailableState == AvailableState.AVAILABLE;
    }

    @JsonProperty
    public boolean isVirtual() {
        return mIsVirtual;
    }

    @JsonProperty
    public boolean isDeveloperDevice() {
        return mIsDeveloperDevice;
    }

    @JsonProperty
    public int getRSSI() {
        return mRSSI;
    }

    @JsonProperty
    public boolean isLinked() {
        return mIsLinked;
    }

    @JsonProperty
    public boolean isDirect() { return mDirect; }

    @JsonIgnore
    public LocationState getLocationState() {
        return mLocationState;
    }

    @JsonIgnore
    public void setLocation(LocationState locationState) {

        if (locationState != null) {
            mLocationState = locationState;
            if (mLocationState.getState().equals(LocationState.State.Valid)) {
                mUpdateSubject.onNext(this);
            }
        }
    }

    @JsonProperty
    public String getProfileID() {
        return mProfileId;
    }

    @JsonIgnore
    public void setProfile(DeviceProfile newProfile) {
        DeviceProfile oldProfile = mProfile;
        mProfile = newProfile;
        mProfileId = newProfile.getId();

        // ideally we could check the profile ids here, but we still can't
        // tell if this is just a presentation update
        if (oldProfile != newProfile) {
            mProfileUpdateSubject.onNext(this);
        }
    }

    @JsonIgnore
    public DeviceProfile getProfile() {
        return mProfile;
    }

    @JsonIgnore
    public DeviceProfile.Attribute getPrimaryOperationAttribute() {
        return mProfile != null ? mProfile.getPrimaryOperationAttribute(getId()) : null;
    }

    @JsonIgnore
    public DeviceProfile.Attribute getAttributeById(int id) {
        return mProfile != null ? mProfile.getAttributeById(id) : null;
    }

    @JsonIgnore
    public DeviceProfile.Presentation getPresentation() {
        return mProfile != null ? mProfile.getPresentation(getId()) : null;
    }

    @JsonProperty("attributes")
    public HashMap<Integer,AttributeDebug> getAttributeValues() {
        HashMap<Integer,AttributeDebug> result = new HashMap<>();
        for (Map.Entry<Integer, AttributeData> attrEntry : mAttributes.entrySet()) {
            AttributeData data = attrEntry.getValue();
            AttributeDebug ad = new AttributeDebug();
            ad.current = data.mCurrentValue != null ? data.mCurrentValue.toString() : null;
            ad.pending = data.mPendingValue != null ? data.mPendingValue.toString() : null;
            result.put(attrEntry.getKey(), ad);
        }
        return result;
    }

    public void writeAttribute(DeviceProfile.Attribute attribute, AttributeValue value) {

        if (mAferoClient == null) return; // for unit tests

        final int attrId = attribute.getId();
        PostActionBody body = new PostActionBody(attrId, value.toString());
        mAferoClient.postAttributeWrite(this, body, WRITE_ATTRIBUTE_RETRY_COUNT, HTTP_LOCKED)
                .subscribe(new ActionObserver(this, Clock.getElapsedMillis()));

        AttributeData data = mAttributes.get(attrId);
        if (data != null) {
            data.mPendingValue = value;
            data.mExpectedUpdateTime = Clock.getElapsedMillis() + WRITE_TIMEOUT_INTERVAL;

            startWaitingForUpdate();
        }

        mLastError = null;

        mUpdateSubject.onNext(this);
    }

    private Observable<RequestResponse> attributeWrite(DeviceRequest[] requests) {

        mLastError = null;

        for (DeviceRequest dr : requests) {
            AttributeData data = mAttributes.get(dr.attrId);
            DeviceProfile.Attribute attribute = getAttributeById(dr.attrId);
            if (data != null && attribute != null) {
                data.mPendingValue = new AttributeValue(dr.value, attribute.getDataType());
                data.mExpectedUpdateTime = Clock.getElapsedMillis() + WRITE_TIMEOUT_INTERVAL;

                startWaitingForUpdate();
            }
        }

        mUpdateSubject.onNext(this);

        if (mOnNextDeviceRequestResponse == null) {
            mOnNextDeviceRequestResponse = new OnNextDeviceRequest(this);
        }
        if (mOnErrorDeviceRequestResponse == null) {
            mOnErrorDeviceRequestResponse = new OnErrorDeviceRequest(this);
        }

        return mAferoClient.postBatchAttributeWrite(this, requests, WRITE_ATTRIBUTE_RETRY_COUNT, HTTP_LOCKED)
            .flatMap(new Func1<RequestResponse[], Observable<RequestResponse>>() {
                @Override
                public Observable<RequestResponse> call(RequestResponse[] requestResponses) {
                    return Observable.from(requestResponses)
                            .doOnNext(mOnNextDeviceRequestResponse);
                }
            })
            .doOnError(mOnErrorDeviceRequestResponse);
    }

    public AttributeValue getAttributeCurrentValue(DeviceProfile.Attribute attribute) {
        AttributeData ad = getAttributeData(attribute);
        return ad != null ? ad.mCurrentValue : null;
    }

    public AttributeValue getAttributePendingValue(DeviceProfile.Attribute attribute) {
        AttributeData ad = getAttributeData(attribute);
        return ad != null ? ad.mPendingValue : null;
    }

    private void updateAttributeValues(int attrId, String value) {
        try {
            AttributeData data = mAttributes.get(attrId);
            if (data == null) {
                data = new AttributeData();
                mAttributes.put(attrId, data);
            }

            DeviceProfile.Attribute attribute = getAttributeById(attrId);
            if (attribute != null && value != null) {
                data.mCurrentValue = new AttributeValue(value, attribute.getDataType());
                data.mPendingValue = new AttributeValue(value, attribute.getDataType());
            }
            data.mExpectedUpdateTime = 0;

            mLastError = null;
        } catch (Exception e) {
            AfLog.e(e);
        }
    }

    public String toJSONString() {
        String result;

        try {
            result = "Device:\n";
            result += JSONUtils.writeValueAsPrettyString(this);
            result += "\n\nProfile:\n";
            result += JSONUtils.writeValueAsPrettyString(getProfile());
            result += "\n\nPresentation:\n";
            result += JSONUtils.writeValueAsPrettyString(getPresentation());
        } catch (JsonProcessingException e) {
            result = e.getMessage();
        }

        return result;
    }

    public void writeModelValue(DeviceProfile.Attribute attribute, BigDecimal newValue) {
        AttributeValue value = getAttributePendingValue(attribute);
        if (value != null) {
            try {
                value.setValue(newValue);
                writeAttribute(attribute, value);
            } catch (Exception e) {
                AfLog.e(e);
            }
        }
    }

    @Override
    public Observable<RequestResponse> writeModelValues(ArrayList<DeviceRequest> req) {
        return attributeWrite(req.toArray(new DeviceRequest[req.size()]));
    }

    @Override
    public void writeModelValue(DeviceProfile.Attribute attribute, AttributeValue value) {
        try {
            writeAttribute(attribute, value);
        } catch (Exception e) {
            AfLog.e(e);
        }
    }

    @Override
    public AttributeValue readPendingValue(DeviceProfile.Attribute attribute) {
        return getAttributePendingValue(attribute);
    }

    @Override
    public AttributeValue readCurrentValue(DeviceProfile.Attribute attribute) {
        return getAttributeCurrentValue(attribute);
    }

    public boolean isRunning() {
        DeviceProfile.Presentation presentation = getPresentation();

        if (presentation != null) {
            for (Map.Entry<Integer,DeviceProfile.AttributeOptions> entry: presentation.getAttributeOptions().entrySet()) {
                DeviceProfile.AttributeOptions ao = entry.getValue();
                DeviceProfile.DisplayRule[] valueOptions = ao.getValueOptions();

                if (valueOptions != null) {

                    DeviceProfile.Attribute attribute = getAttributeById(entry.getKey());
                    ApplyParams apply = new ApplyParams();
                    AttributeValue av = getAttributeCurrentValue(attribute);

                    if (av != null) {
                        for (DeviceProfile.DisplayRule rule : valueOptions) {
                            AttributeValue matchValue = new AttributeValue(rule.match, attribute.getDataType());
                            if (matchValue.compareTo(av) == 0) {
                                apply.putAll(rule.apply);
                            }
                        }
                    }

                    Object value = apply.get("state");
                    if (value != null && value instanceof String) {
                        boolean isRunning = "running".equalsIgnoreCase((String)value);
                        if (isRunning) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void putTag(String key, String value) {
        HashMap<String,String> tags = getTags();
        tags.put(key, value);
    }

    public String getTag(String key) {
        return getTags().get(key);
    }

    public void putSelectedGroupKey(String key) {
        putTag(TAG_SELECTED_GROUP, key);
    }

    public String getSelectedGroupKey() {
        return getTag(TAG_SELECTED_GROUP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceModel that = (DeviceModel) o;

        return mId != null ? mId.equals(that.mId) : that.mId == null;

    }

    @Override
    public int hashCode() {
        return mId != null ? mId.hashCode() : 0;
    }


    // non-public ------------------------------------------------------------------

    void update(DeviceStatus deviceStatus) {
        if (updateStatus(deviceStatus)) {
            mUpdateSubject.onNext(this);
        }
    }

    void onError(DeviceError deviceError) {
        // Only report hub errors that are the result of requests (See ANDROID-580)
        if (deviceError.requestId != 0) {
            MetricUtil.getInstance().end(deviceError.requestId, Clock.getElapsedMillis(), false, ConclaveMessage.Metric.FailureReason.HUB_ERROR);
        }

        mLastError = deviceError;
        mErrorSubject.onNext(deviceError);
    }

    void onMute(DeviceMute deviceMute) {
        mLastError = deviceMute;
        mErrorSubject.onNext(deviceMute);
    }

    void onOTA(OTAInfo otaInfo) {
        AfLog.d("DeviceModel.onOTA" + otaInfo);

        mOTAState = otaInfo.state;
        mOTAInProgress = true;
        mOTAProgress = otaInfo.getProgress();

        resetOTAWatchdog();
        mUpdateSubject.onNext(this);
    }

    void updateLocation() {
        setLocation(new LocationState(LocationState.State.Invalid));
        mAferoClient.getDeviceLocation(this)
            .subscribe(new Observer<Location>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    AfLog.e("Unable to get location " + e);
                }

                @Override
                public void onNext(Location location) {
                    LocationState locationState = new LocationState(location);
                    setLocation(locationState);
                }
            });
    }

    private void onError(Throwable e) {
        ApiClientError error = new ApiClientError();
        error.code = mAferoClient.getStatusCode(e);

        final ConclaveMessage.Metric.FailureReason fr = e instanceof SocketTimeoutException
                ? ConclaveMessage.Metric.FailureReason.SERVICE_API_TIMEOUT
                : ConclaveMessage.Metric.FailureReason.SERVICE_API_ERROR;

        MetricUtil.getInstance().reportError(getId(), 0L, fr);

        mLastError = error;
        mErrorSubject.onNext(error);
    }

    private void onError(int statusCode) {
        ApiClientError error = new ApiClientError();
        error.code = statusCode;

        MetricUtil.getInstance().reportError(getId(), 0L, ConclaveMessage.Metric.FailureReason.SERVICE_API_ERROR);

        mLastError = error;
        mErrorSubject.onNext(error);
    }

    private void startWaitingForUpdate() {
        mPendingWriteSubscription = Observable.just(this)
            .delay(WRITE_TIMEOUT_INTERVAL, TimeUnit.MILLISECONDS)
            .subscribe(new RxUtils.WeakAction1<DeviceModel, DeviceModel>(this) {
                @Override
                public void call(DeviceModel strongRef, DeviceModel deviceModel) {
                    deviceModel.onUpdateTimeout();
                }
            });
    }

    private void onUpdateTimeout() {
        MetricUtil.getInstance().purgeTimedOutWrites();
        mUpdateSubject.onNext(this);
    }

    private boolean updateStatus(DeviceStatus deviceStatus) {
        boolean hasChanged = false;

        if (deviceStatus.rssi != null) {
            mRSSI = deviceStatus.rssi;
            hasChanged = true;
        }

        if (deviceStatus.linked != null) {
            mIsLinked = deviceStatus.linked;
            hasChanged = true;
        }

        if (deviceStatus.available != null) {
            mAvailableState = deviceStatus.available ? AvailableState.AVAILABLE : AvailableState.UNAVAILABLE;
            hasChanged = true;
        }

        if (deviceStatus.direct != null) {
            mDirect = deviceStatus.direct;
            hasChanged = true;
        }

        return hasChanged;
    }

    private void resetOTAWatchdog() {
        cancelOTAWatchdog();

        mOTAWatchdogSubscription = Observable.just(mOTAProgress)
                .delay(OTA_WATCHDOG_DELAY, TimeUnit.MILLISECONDS)
                .subscribe(new RxUtils.WeakAction1<Integer, DeviceModel>(this) {
                    @Override
                    public void call(DeviceModel deviceModel, Integer progress) {
                        deviceModel.onOTAWatchdogFired(progress);
                    }
                });
    }

    private void cancelOTAWatchdog() {
        mOTAWatchdogSubscription = RxUtils.safeUnSubscribe(mOTAWatchdogSubscription);
    }

    private void onOTAWatchdogFired(int oldProgress) {
        AfLog.d("DeviceModel.onOTAWatchdogFired");

        mOTAWatchdogSubscription = RxUtils.safeUnSubscribe(mOTAWatchdogSubscription);

        if (oldProgress == mOTAProgress) {
            onOTAStop();
        }
    }

    private static class ActionObserver extends RxUtils.WeakObserver<ActionResponse, DeviceModel> {

        long timestamp;

        ActionObserver(DeviceModel strongRef, long t) {
            super(strongRef);
            timestamp = t;
        }

        @Override
        public void onCompleted(DeviceModel deviceModel) {

        }

        @Override
        public void onError(DeviceModel deviceModel, Throwable e) {
            deviceModel.onError(e);
        }

        @Override
        public void onNext(DeviceModel deviceModel, ActionResponse response) {
            MetricUtil.getInstance().begin(response.requestId, deviceModel.getId(), timestamp);
        }
    }

    private static class OnNextDeviceRequest extends RxUtils.WeakAction1<RequestResponse, DeviceModel> {

        OnNextDeviceRequest(DeviceModel strongRef) {
            super(strongRef);
        }

        @Override
        public void call(DeviceModel deviceModel, RequestResponse requestResponse) {
            if (requestResponse.isSuccess()) {
                MetricUtil.getInstance().begin(requestResponse.requestId, deviceModel.getId(), requestResponse.timestampMs);
            } else {
                deviceModel.onError(requestResponse.statusCode);
            }
        }
    }

    private static class OnErrorDeviceRequest extends RxUtils.WeakAction1<Throwable, DeviceModel> {

        OnErrorDeviceRequest(DeviceModel strongRef) {
            super(strongRef);
        }

        @Override
        public void call(DeviceModel deviceModel, Throwable t) {
            deviceModel.onError(t);
        }
    }

    private AttributeData getAttributeData(DeviceProfile.Attribute attribute) {
        if (attribute == null) {
            return null;
        }

        final int attrId = attribute.getId();
        AttributeData data = mAttributes.get(attrId);
        if (data == null) {
            data = new AttributeData();
            data.mCurrentValue = new AttributeValue(attribute.getDataType());
            data.mPendingValue = new AttributeValue(attribute.getDataType());
            mAttributes.put(attrId, data);
        }

        return data;
    }

    private HashMap<String,String> mTags;
    private static final String TAG_SELECTED_GROUP = "selected-group-id";

    private HashMap<String,String> getTags() {
        if (mTags == null) {
            mTags = new HashMap<>();
        }
        return mTags;
    }
}
