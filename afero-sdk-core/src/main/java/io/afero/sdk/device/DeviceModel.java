/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.AferoError;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.DeviceStatus;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.LocationState;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.DeviceEventSource;
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
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;


/**
 * This class manages the attribute state and events associated with a particular instance of an
 * Afero peripheral device.
 */
public final class DeviceModel {

    public static class ApiClientError implements AferoError {
        public int code;
        public String status;
    }

    public enum UpdateState {
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
    private static final int WRITE_ATTRIBUTE_RETRY_COUNT = 4;

    @SuppressWarnings("WeakerAccess")
    private class AttributeData {
        public AttributeValue mCurrentValue;
        public AttributeValue mPendingValue;
        long mExpectedUpdateTime;
        long mUpdatedTimeStamp;
    }

    @SuppressWarnings("WeakerAccess")
    public static class AttributeDebug {
        public String pending;
        public String current;
    }

    private static final int HTTP_LOCKED = 423; // https://tools.ietf.org/html/rfc4918#section-11.3
    private static final long OTA_WATCHDOG_TIMEOUT_SECONDS = 30L;

    private Subscription mPendingWriteSubscription;

    private final String mId;
    private final AferoClient mAferoClient;
    private final HashMap<Integer,AttributeData> mAttributes = new HashMap<>();

    private String mName;

    private String mProfileId;
    private DeviceProfile mProfile;

    private AvailableState mAvailableState = AvailableState.NONE;

    private final PublishSubject<DeviceSync> mDeviceSyncPreUpdateSubject = PublishSubject.create();
    private final PublishSubject<DeviceSync> mDeviceSyncPostUpdateSubject = PublishSubject.create();
    private final PublishSubject<AferoError> mErrorSubject = PublishSubject.create();
    private final PublishSubject<DeviceModel> mProfileUpdateSubject = PublishSubject.create();
    private final PublishSubject<DeviceModel> mUpdateSubject = PublishSubject.create();
    private final Observable<DeviceModel> mUpdateObservable;

    private OTAWatcher mOTAWatcher;
    private Subscription mOTASubscription;

    private DeviceDataMigrator mDataMigrator = new DeviceDataMigrator(this);
    private Subscription mMigrationSubscription;

    private int mRSSI;
    private boolean mIsLinked;
    private boolean mDirect;
    private boolean mIsVirtual;
    private final boolean mIsDeveloperDevice;

    private LocationState mLocationState = new LocationState(LocationState.State.INVALID);
    private final TimeZoneValue mTimeZoneValue = new TimeZoneValue();

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

    /**
     * @return globally unique identifier for this device.
     */
    @JsonProperty
    public String getId() {
        return mId;
    }

    /**
     * Get a friendly user-readable name for this device.
     *
     * @return String set via {@link #setName(String)},
     * if null or empty returns {@link DeviceProfile.Presentation#getLabel()},
     * if null or empty returns {@link DeviceProfile#getDeviceType()},
     * if null or empty returns {@link #getProfileID()},
     * if null returns empty string.
     */
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

    /**
     * Sets the friendly name for this device. Sets only the local field, does not affect Afero Cloud.
     *
     * @param name String containing friendly name.
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return {@code true} if the device is currently
     */
    @JsonProperty
    public boolean isAvailable() {
        return mAvailableState == AvailableState.AVAILABLE;
    }

    /**
     * @return {@code true} if the device is a non-physical cloud-based instance
     */
    @JsonProperty
    public boolean isVirtual() {
        return mIsVirtual;
    }

    /**
     * @return {@code true} if the device is a development version such as Modulo-1 or Modulo-2
     */
    @JsonProperty
    public boolean isDeveloperDevice() {
        return mIsDeveloperDevice;
    }

    /**
     * @return integer represent the signal strength of the device's active wireless connection.
     */
    @JsonProperty
    public int getRSSI() {
        return mRSSI;
    }

    /**
     * @return {@code true} if the device has completed the secure handshake with the Afero Cloud.
     */
    @JsonProperty
    public boolean isLinked() {
        return mIsLinked;
    }

    /**
     * @return {@code true} if this device communicates directly with the Afero Cloud; {@code false}
     * if the device uses a bridge.
     */
    @JsonProperty
    public boolean isDirect() { return mDirect; }

    /**
     * Get the location set for this device. This is optionally set via the Afero app when the
     * device is associated with an account.
     *
     * @return {@link Observable} that emits {@link LocationState} attached to this device.
     */
    @JsonIgnore
    public Observable<LocationState> getLocationState() {
        if (!LocationState.State.VALID.equals(mLocationState.getState())) {
            return mAferoClient.getDeviceLocation(this)
                    .map(new Func1<Location, LocationState>() {
                        @Override
                        public LocationState call(Location location) {
                            return new LocationState(location);
                        }
                    })
                    .doOnNext(new Action1<LocationState>() {
                        @Override
                        public void call(LocationState locationState) {
                            setLocationState(locationState);
                        }
                    });
        }

        return Observable.just(mLocationState);
    }

    /**
     * Calls the Afero Cloud API to set the location for this device.
     *
     * @return {@link Observable} that emits the {@link Location} attached to this device.
     */
    @JsonIgnore
    public Observable<Location> setLocation(Location location) {
        return mAferoClient.putDeviceLocation(getId(), location)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        setLocationState(new LocationState(LocationState.State.INVALID));
                    }
                })
                .doOnNext(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        setLocationState(new LocationState(location));
                    }
                });
    }

    /**
     * @return the {@link TimeZone} in which the device resides.
     */
    public Observable<TimeZone> getTimeZone() {

        if (mTimeZoneValue.getState().equals(TimeZoneValue.State.SET)) {
            return Observable.just(mTimeZoneValue.getTimeZone());
        }

        return mAferoClient.getDeviceTimeZone(this)
                .doOnNext(new Action1<TimeZone>() {
                    @Override
                    public void call(TimeZone timeZone) {
                        mTimeZoneValue.setTimeZone(timeZone);
                    }
                });
    }

    /**
     * Associates a given TimeZone with this device
     *
     * @param tz TimeZone to associate with this device
     * @return Observable that emits the specfied TimeZone when successfully stored via the Afero Cloud
     */
    public Observable<TimeZone> setTimeZone(TimeZone tz) {
        return mAferoClient.putDeviceTimeZone(this, tz)
                .map(new RxUtils.Mapper<Void, TimeZone>(tz))
                .doOnNext(new Action1<TimeZone>() {
                    @Override
                    public void call(TimeZone timeZone) {
                        mTimeZoneValue.setTimeZone(timeZone);
                    }
                });
    }

    public boolean isTimeZoneSet() {
        TimeZoneValue.State timeZoneState = mTimeZoneValue.getState();
        return !timeZoneState.equals(TimeZoneValue.State.NOT_SET);
    }

    /**
     * @return String containing the ID of the {@link DeviceProfile} associated with this device.
     */
    @JsonProperty
    public String getProfileID() {
        return mProfileId;
    }

    /**
     * @return {@link DeviceProfile} associated with this device.
     */
    @JsonIgnore
    public DeviceProfile getProfile() {
        return mProfile;
    }

    /**
     * @return {@link DeviceProfile.Presentation} associated with this device, or null if none exists.
     */
    @JsonIgnore
    public DeviceProfile.Presentation getPresentation() {
        return mProfile != null ? mProfile.getPresentation(getId()) : null;
    }

    /**
     * @param id unique identifier of the desired Attribute.
     * @return {@link DeviceProfile.Attribute} with the specifed id, or {@code null} no such Attribute was found.
     */
    @JsonIgnore
    public DeviceProfile.Attribute getAttributeById(int id) {
        return mProfile != null ? mProfile.getAttributeById(id) : null;
    }

    /**
     * This "primary operation" concept is application defined. It can also be thought of as the "default" attribute.
     *
     * @return {@link DeviceProfile.Attribute} defined via <a href="https://developer.afero.io/docs/en/?target=Projects.html">Profile Editor</a>
     * as the primary operation.
     */
    @JsonIgnore
    public DeviceProfile.Attribute getPrimaryOperationAttribute() {
        return mProfile != null ? mProfile.getPrimaryOperationAttribute(getId()) : null;
    }

    /**
     * Gets an {@link Observable} that emits this DeviceModel whenever a change occurs in the
     * connected state of the device, or the values of any of its {@link DeviceProfile.Attribute}s.
     *
     * @return {@link Observable}
     */
    @JsonIgnore
    public rx.Observable<DeviceModel> getUpdateObservable() {
        return mUpdateObservable;
    }

    /**
     * Gets an {@link Observable} that emits a {@link DeviceSync} whenever this device receives
     * one via the {@link DeviceEventSource}. {@link Observer#onNext(Object)} will be called *before*
     * the DeviceModel is updated as a result of the DeviceSync event.
     *
     * @return {@link Observable}
     */
    @JsonIgnore
    public rx.Observable<DeviceSync> getDeviceSyncPreUpdateObservable() {
        return mDeviceSyncPreUpdateSubject;
    }

    /**
     * Gets an {@link Observable} that emits a {@link DeviceSync} whenever this device receives
     * one via the {@link DeviceEventSource}. {@link Observer#onNext(Object)} will be called *after*
     * the DeviceModel is updated as a result of the DeviceSync event.
     *
     * @return {@link Observable}
     */
    @JsonIgnore
    public Observable<DeviceSync> getDeviceSyncPostUpdateObservable() {
        return mDeviceSyncPostUpdateSubject;
    }

    /**
     * Gets an {@link Observable} that emits a {@link AferoError} whenever this device receives
     * one via the {@link DeviceEventSource}. The AferoError may be an instance of {@link DeviceError},
     * {@link ApiClientError}, or {@link DeviceMute}.
     *
     * @return {@link Observable}
     */
    @JsonIgnore
    public rx.Observable<AferoError> getErrorObservable() {
        return mErrorSubject.onBackpressureBuffer();
    }

    /**
     * Gets an {@link Observable} that emits this {@link DeviceModel} whenever the
     * {@link DeviceProfile} is replaced as a result of an OTA update. This usually occurs during
     * development while using the <a href="https://developer.afero.io/docs/en/?target=Projects.html">Profile Editor</a>
     * to make changes to the DeviceProfile.
     *
     * @return {@link Observable}
     * @see <a href="https://developer.afero.io/docs/en/?target=Publish.html">Profile Editor: Publish Your Project</a>
     */
    @JsonIgnore
    public rx.Observable<DeviceModel> getProfileObservable() {
        return mProfileUpdateSubject;
    }

    /**
     * Gets the {@link UpdateState} of this device which indicates if any
     * {@link DeviceProfile.Attribute} is either waiting for an update or has timed out while
     * waiting after a write operation.
     *
     * @return {@link UpdateState}
     * @see #writeAttributes()
     */
    @JsonProperty
    public UpdateState getState() {
        UpdateState updateState = UpdateState.NORMAL;
        long now = Clock.getElapsedMillis();

        for (AttributeData data : mAttributes.values()) {
            if (data.mExpectedUpdateTime != 0) {
                if (now > data.mExpectedUpdateTime) {
                    updateState = UpdateState.UPDATE_TIMED_OUT;
                } else {
                    updateState = UpdateState.WAITING_FOR_UPDATE;
                    break;
                }
            }
        }

        return updateState;
    }

    /**
     * @return true if this {@link DeviceModel} is currently receiving an OTA update; false otherwise.
     */
    public boolean isOTAInProgress() {
        return mOTAWatcher != null;
    }

    /**
     * @return {@link Observable} that emits the percentage of completion of any OTA in progress.
     */
    @JsonIgnore
    public Observable<Integer> getOTAProgress() {
        return mOTAWatcher != null ? mOTAWatcher.getProgressObservable() : Observable.<Integer>empty();
    }

    /**
     * @return The most recent {@link AferoError} received for this device.
     * @see #getErrorObservable()
     * @see #clearLastError()
     */
    @JsonIgnore
    public AferoError getLastError() {
        return mLastError;
    }

    /**
     * Clears the {@link AferoError} returned by {@link #getLastError()}
     */
    public void clearLastError() {
        if (mLastError != null) {
            mLastError = null;
            mUpdateSubject.onNext(this);
        }
    }

    /**
     * Writes new values to one or more device attributes. When the operation completes successfully
     * the new values have been confirmed by the physical device. It is possible for some of the
     * attribute writes to succeed and others to fail.
     *
     * <p>
     * Example:
     * <pre><code>
     *     deviceModel.writeAttributes()
     *         .put(powerAttrId, powerValue)
     *         .put(modeAttrId, modeValue)
     *         .commit()
     *         .subscribe(new Observer&lt;AttributeWriter.Result&gt;() {
     *              &#64;Override
     *              public void onNext(AttributeWriter.Result result) {
     *                  if (result.isSuccess()) {
     *                      // the physical device confirmed the write
     *                  } else {
     *                      // the write for one of the attributes failed
     *                  }
     *              }
     *
     *              &#64;Override
     *              public void onError(Throwable t) {
     *                   // call failed, log the error
     *                   AfLog.e(t);
     *              }
     *
     *              &#64;Override
     *              public void onCompleted() {
     *                  // write operation completed
     *              }
     *         });
     * </code></pre>
     * </p>
     *
     * @return {@link AttributeWriter} that is used to compose, initiate, and monitor the write operation.
     * @see AttributeWriter
     */
    public AttributeWriter writeAttributes() {
        return new AttributeWriter(this);
    }

    /**
     * Gets the local cached attribute value last received from the Afero Cloud.
     *
     * @param attribute {@link DeviceProfile.Attribute}
     * @return {@link AttributeValue} of the specified Attribute.
     */
    public AttributeValue getAttributeCurrentValue(DeviceProfile.Attribute attribute) {
        AttributeData ad = getAttributeData(attribute);
        return ad != null ? ad.mCurrentValue : null;
    }

    /**
     * Gets the local cached attribute value that is in the process of being written to the physical
     * device as a result of a call to {@link #writeAttributes()}.
     *
     * @param attribute {@link DeviceProfile.Attribute}
     * @return {@link AttributeValue} of the specified Attribute.
     */
    public AttributeValue getAttributePendingValue(DeviceProfile.Attribute attribute) {
        AttributeData ad = getAttributeData(attribute);
        return ad != null ? ad.mPendingValue : null;
    }

    /**
     * @param attribute {@link DeviceProfile.Attribute}
     */
    public void cancelAttributePendingValue(DeviceProfile.Attribute attribute) {
        AttributeData ad = getAttributeData(attribute);
        if (ad != null && ad.mCurrentValue != null) {
            ad.mPendingValue = new AttributeValue(ad.mCurrentValue.toString(), attribute.getDataType());
            ad.mExpectedUpdateTime = 0;
        }
    }

    /**
     * Get the timestamp when the value of the specified {@link DeviceProfile.Attribute} was last
     * updated in the Afero Cloud.
     *
     * @param attribute {@link DeviceProfile.Attribute}
     * @return long timestamp in milliseconds
     */
    public long getAttributeUpdatedTime(DeviceProfile.Attribute attribute) {
        AttributeData ad = getAttributeData(attribute);
        return ad != null ? ad.mUpdatedTimeStamp : 0;
    }

    @Deprecated
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

    @Override
    public String toString() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final String thisId = mId;
        final String thatId = ((DeviceModel)o).mId;

        return thisId != null ? thisId.equals(thatId) : thatId == null;

    }

    @Override
    public int hashCode() {
        return mId != null ? mId.hashCode() : 0;
    }


    // non-public ------------------------------------------------------------------

    void setProfile(DeviceProfile newProfile) {
        DeviceProfile oldProfile = mProfile;
        mProfile = newProfile;
        mProfileId = newProfile.getId();

        // ideally we could check the profile ids here, but we still can't
        // tell if this is just a presentation update
        if (oldProfile != newProfile) {
            mProfileUpdateSubject.onNext(this);
        }
    }

    void onWriteStart(Collection<WriteRequest> requests) {
        mLastError = null;

        for (WriteRequest dr : requests) {
            AttributeData data = mAttributes.get(dr.attrId);
            DeviceProfile.Attribute attribute = getAttributeById(dr.attrId);
            if (data != null && attribute != null) {
                data.mPendingValue = new AttributeValue(dr.value, attribute.getDataType());
                data.mExpectedUpdateTime = Clock.getElapsedMillis() + WRITE_TIMEOUT_INTERVAL;

                startWaitingForUpdate();
            }
        }

        mUpdateSubject.onNext(this);
    }

    void onWriteResult(AttributeWriter.Result writeResult) {
        // as results come in cancel the timers on corresponding attributes
        // or let existing logic in update handle it?
    }

    Observable<WriteResponse[]> postAttributeWriteRequests(Collection<WriteRequest> requests) {

        mLastError = null;

        for (WriteRequest dr : requests) {
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

        WriteRequest[] reqArray = requests.toArray(new WriteRequest[requests.size()]);
        return mAferoClient.postBatchAttributeWrite(this, reqArray, WRITE_ATTRIBUTE_RETRY_COUNT, HTTP_LOCKED);
    }

    Observable<WriteResponse[]> postBatchAttributeWrite(Collection<WriteRequest> requests, int retryCount, int statusCode) {
        return mAferoClient.postBatchAttributeWrite(this, requests.toArray(new WriteRequest[requests.size()]), retryCount, statusCode);
    }

    /**
     * Used for testing.
     */
    void writeAttribute(DeviceProfile.Attribute attribute, AttributeValue value) {

        if (mAferoClient == null) return;

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

    void update(DeviceSync deviceSync) {

        mDeviceSyncPreUpdateSubject.onNext(deviceSync);

        if (deviceSync.hasRequestId()) {
            MetricUtil.getInstance().end(deviceSync.requestId, Clock.getElapsedMillis(), true, null);
        }

        final boolean hasValidValues = deviceSync.hasValidAttributeValues();
        boolean hasChanged = false;

        if (hasValidValues && deviceSync.attributes != null) {
            hasChanged = true;
            for (DeviceSync.AttributeEntry ae : deviceSync.attributes) {
                updateAttributeValues(ae);
            }
        }

        if (hasValidValues && deviceSync.attribute != null) {
            hasChanged = true;
            updateAttributeValues(deviceSync.attribute);
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

        if (deviceSync.timezone != null && deviceSync.timezone.timezone != null) {
            mTimeZoneValue.setTimeZone(TimeZone.getTimeZone(deviceSync.timezone.timezone));
        }

        mDeviceSyncPostUpdateSubject.onNext(deviceSync);

        if (hasChanged) {
            mUpdateSubject.onNext(this);
        }

        runDataMigrations();
    }

    void update(DeviceStatus deviceStatus) {
        if (updateStatus(deviceStatus)) {
            mUpdateSubject.onNext(this);

            runDataMigrations();
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

    void invalidateLocationState() {
        setLocationState(new LocationState(LocationState.State.INVALID));
    }

    void invalidateTimeZone() {
        mTimeZoneValue.invalidate();
        mUpdateSubject.onNext(this);
    }

    void onOTA(OTAInfo otaInfo) {
        AfLog.d("DeviceModel.onOTA: " + otaInfo);

        if (mOTAWatcher == null && otaInfo.getState() != OTAInfo.OtaState.STOP) {
            mOTAWatcher = new OTAWatcher(OTA_WATCHDOG_TIMEOUT_SECONDS);
            mOTASubscription = mOTAWatcher.getProgressObservable()
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            onOTAStop();
                        }
                    })
                    .subscribe();
            mUpdateSubject.onNext(this);
        }

        if (mOTAWatcher != null) {
            mOTAWatcher.onOTA(otaInfo);
        }
    }

    private void onOTAStop() {
        AfLog.d("DeviceModel.onOTAStop");

        mOTASubscription = RxUtils.safeUnSubscribe(mOTASubscription);

        if (mOTAWatcher != null) {
            mOTAWatcher = null;
            mUpdateSubject.onNext(this);
        }
    }

    private void runDataMigrations() {
        synchronized (this) {
            if (isAvailable() && mDataMigrator != null && mMigrationSubscription == null) {

                Observable<DeviceDataMigrator> migratorObservable = mDataMigrator.runMigrations();
                if (migratorObservable == null) {
                    mDataMigrator = null;
                    return;
                }

                mMigrationSubscription = migratorObservable
                    .subscribe(new Observer<DeviceDataMigrator>() {
                        @Override
                        public void onCompleted() {
                            AfLog.i("DeviceModel.runDataMigrations: onCompleted");
                            mMigrationSubscription = null;
                            mDataMigrator = null;
                        }

                        @Override
                        public void onError(Throwable e) {
                            mMigrationSubscription = null;
                            AfLog.e("DeviceModel.runDataMigrations.onError: " + e.getMessage());
                        }

                        @Override
                        public void onNext(DeviceDataMigrator ddm) {}
                    });
            }
        }
    }

    private void setLocationState(LocationState locationState) {
        mLocationState = locationState;
        if (mLocationState.getState().equals(LocationState.State.VALID)) {
            mUpdateSubject.onNext(this);
        }
    }

    private void updateAttributeValues(DeviceSync.AttributeEntry ae) {
        try {
            AttributeData data = mAttributes.get(ae.id);
            if (data == null) {
                data = new AttributeData();
                mAttributes.put(ae.id, data);
            }

            DeviceProfile.Attribute attribute = getAttributeById(ae.id);
            if (attribute != null && ae.value != null) {
                data.mCurrentValue = new AttributeValue(ae.value, attribute.getDataType());
                data.mPendingValue = new AttributeValue(ae.value, attribute.getDataType());

                if (ae.updatedTimestamp != 0) {
                    data.mUpdatedTimeStamp = ae.updatedTimestamp;
                }
            }
            data.mExpectedUpdateTime = 0;

            mLastError = null;
        } catch (Exception e) {
            AfLog.e(e);
        }
    }

    void onError(Throwable e) {
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
            .subscribe(new UpdateTimeoutAction(this));
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

    private static class OnNextDeviceRequest extends RxUtils.WeakAction1<WriteResponse, DeviceModel> {

        OnNextDeviceRequest(DeviceModel strongRef) {
            super(strongRef);
        }

        @Override
        public void call(DeviceModel deviceModel, WriteResponse writeResponse) {
            if (writeResponse.isSuccess()) {
                MetricUtil.getInstance().begin(writeResponse.requestId, deviceModel.getId(), writeResponse.timestampMs);
            } else {
                deviceModel.onError(writeResponse.statusCode);
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

    private static class UpdateTimeoutAction extends RxUtils.WeakAction1<DeviceModel, DeviceModel> {

        UpdateTimeoutAction(DeviceModel deviceModel) {
            super(deviceModel);
        }

        @Override
        public void call(DeviceModel deviceModel, DeviceModel nextDeviceModel) {
            nextDeviceModel.onUpdateTimeout();
        }
    }
}
