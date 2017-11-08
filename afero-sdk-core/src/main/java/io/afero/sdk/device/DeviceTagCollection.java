/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

class DeviceTagCollection {

    /**
     * This class represents a key/value object that can be attached to a {@link DeviceModel}.
     */
    public static class Tag {
        private String mKey;
        private String mValue;
        private String mDeviceTagId;

        private Tag() {}

        Tag(String k, String v) {
            mKey = k;
            mValue = v;
        }

        Tag(String tagData) throws IOException {
            Tag tag = JSONUtils.readValue(tagData, Tag.class);
            mKey = tag.mKey;
            mValue = tag.mValue;
        }

        @JsonProperty("k")
        public String getKey() {
            return mKey;
        }

        void setKey(String k) {
            mKey = k;
        }

        @JsonProperty("v")
        public String getValue() {
            return mValue;
        }

        void setValue(String v) {
            mValue = v;
        }

        @JsonIgnore
        String getDeviceTagId() {
            return mDeviceTagId;
        }

        void setDeviceTagId(String id) {
            mDeviceTagId = id;
        }

        String serialize() throws JsonProcessingException {
            return JSONUtils.writeValueAsString(this);
        }
    }

    public enum TagAction {
        ADD,
        UPDATE,
        DELETE;

        TagAction fromString(String s) {
            valueOf(s);
            return null;
        }
    }

    public static class TagEvent {

        public final TagAction action;
        public final Tag tag;

        private TagEvent(TagAction a, Tag t) {
            action = a;
            tag = t;
        }
    }


    private final DeviceModel mDeviceModel;
    private final AferoClient mAferoClient;

    private final HashMap<String, Tag> mTags = new HashMap<>();
    private final PublishSubject<TagEvent> mTagSubject = PublishSubject.create();


    DeviceTagCollection(DeviceModel deviceModel) {
        mDeviceModel = deviceModel;
        mAferoClient = deviceModel.getAferoClient();
    }

    /**
     * Attaches a tag key/value to this device persistently via the Afero Cloud. The tag is removed
     * if the device is disassociated from the account.
     *
     * @param key String specifying a unique identifier for the new tag
     * @param value String containing arbitrary value for the new tag
     * @return Observable that emits the new {@link Tag}
     */
    public Observable<Tag> saveTag(String key, String value) {

        Tag tag = mTags.get(key);
        Observable<DeviceTag> tagObservable;

        try {
            if (tag == null) {
                tag = new Tag(key, value);
                tagObservable = mAferoClient.postDeviceTag(mDeviceModel.getId(), tag.serialize());
            } else {
                Tag newTag = new Tag(key, value);
                tagObservable = mAferoClient.putDeviceTag(mDeviceModel.getId(), tag.getDeviceTagId(), newTag.serialize());
            }
        } catch (JsonProcessingException e) {
            return Observable.error(e);
        }

        return tagObservable.flatMap(
                new Func1<DeviceTag, Observable<Tag>>() {
                    @Override
                    public Observable<Tag> call(DeviceTag deviceTag) {
                        try {
                            Tag newTag = addTagById(deviceTag.deviceTagId, deviceTag.value);
                            return Observable.just(newTag);
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                });
    }

    /**
     * Attaches a temporary key/value tag to this DeviceModel.
     * The tag does *not* persist across sessions.
     * Replacing an existing persistent tag with this method,
     * also only lasts for the current session.
     *
     * @param key String specifying a unique identifier for the new tag
     * @param value String containing arbitrary value for the new tag
     * @see #saveTag(String, String)
     */
    public void putTag(String key, String value) {
        mTags.put(key, new Tag(key, value));
    }

    /**
     * Deletes a tag from both local and persistent cloud storage.
     *
     * @param key Unique indentifier of the tag.
     * @return {@link Tag} object that was removed; null if no such tag was found.
     */
    public Tag deleteTag(String key) {
        Tag tag = mTags.remove(key);
        if (tag != null && tag.getDeviceTagId() != null) {
            mAferoClient.deleteDeviceTag(mDeviceModel.getId(), tag.getDeviceTagId())
                    .subscribe(new RxUtils.IgnoreResponseObserver<Void>());
        }
        return tag;
    }

    /**
     * Retrieves the value of a tag attached to this device via {@link #putTag(String, String)}
     * or {@link #saveTag(String, String)}
     *
     * @param key Unique indentifier of the tag.
     * @return String containing the value of the tag.
     * @see #saveTag(String, String)
     * @see #putTag(String, String)
     */
    public String getTag(String key) {
        Tag tag = mTags.get(key);
        return tag != null ? tag.getValue() : null;
    }

    /**
     * @return {@link Iterable} for the collection of tags currently attached to the device.
     */
    @JsonIgnore
    public Iterable<Tag> getTags() {
        return mTags.values();
    }

    /**
     * Setter for DeviceTags during DeviceModel JSON deserialization
     *
     * @param deviceTags Array of {@link DeviceTag} objects attached to this DeviceModel
     */
    @JsonProperty
    public void setDeviceTags(DeviceTag[] deviceTags) {
        if (deviceTags.length == 0) {
            if (mTags != null) {
                mTags.clear();
            }
            return;
        }

        HashMap<String, Tag> tags = mTags;

        for (DeviceTag dt : deviceTags) {
            Tag tag = null;

            try {
                tag = new Tag(dt.value);
            } catch (IOException e) {
                e.printStackTrace();
                // ignore
            }

            if (tag == null) {
                tag = new Tag(dt.deviceTagId, dt.value);
            }
            tag.setDeviceTagId(dt.deviceTagId);

            tags.put(tag.getKey(), tag);
        }
    }

    void invalidateTag(String actionString, String deviceTagId, String deviceTagValue) {
        try {
            TagAction action = TagAction.valueOf(actionString.toUpperCase(Locale.ROOT));
            switch (action) {
                case ADD:
                    addTagById(deviceTagId, deviceTagValue);
                    break;
                case UPDATE:
                    updateTagById(deviceTagId, deviceTagValue);
                    break;
                case DELETE:
                    deleteTagById(deviceTagId);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Tag getTagInternal(String key) {
        return mTags.get(key);
    }

    Tag getTagById(String deviceTagId) {

        for (Tag tag : mTags.values()) {
            if (tag.getDeviceTagId() != null && tag.getDeviceTagId().equals(deviceTagId)) {
                return tag;
            }
        }

        return null;
    }

    private Tag addTagById(String deviceTagId, String deviceTagData) throws IOException {
        Tag newTag = new Tag(deviceTagData);
        newTag.setDeviceTagId(deviceTagId);

        // store the new tag in the local collection
        mTags.put(newTag.getKey(), newTag);

        mTagSubject.onNext(new TagEvent(TagAction.ADD, newTag));

        return newTag;
    }

    private Tag updateTagById(String deviceTagId, String deviceTagData) throws IOException {
        Tag tag = new Tag(deviceTagData);
        tag.setDeviceTagId(deviceTagId);

        // store the new tag in the local collection
        mTags.put(tag.getKey(), tag);

        mTagSubject.onNext(new TagEvent(TagAction.UPDATE, tag));

        return tag;
    }

    private Tag deleteTagById(String deviceTagId) throws IOException {
        Tag tag = getTagById(deviceTagId);
        if (tag != null) {
            tag = mTags.remove(tag.getKey());
        }

        mTagSubject.onNext(new TagEvent(TagAction.DELETE, tag));

        return tag;
    }

}
