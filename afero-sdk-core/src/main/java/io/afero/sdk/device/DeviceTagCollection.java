/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import java.util.HashMap;
import java.util.Locale;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

class DeviceTagCollection {

    /**
     * This class represents a collection of key/value object "tag" that can be attached
     * to a {@link DeviceModel}.
     */
    public static class Tag {

        private final DeviceTag mDeviceTag;

        private Tag() {
            mDeviceTag = new DeviceTag();
        }

        Tag(DeviceTag dt) {
            mDeviceTag = dt;
        }

        Tag(String key, String value) {
            mDeviceTag = new DeviceTag(key, value);
        }

        public String getKey() {
            return mDeviceTag.key;
        }

        public String getValue() {
            return mDeviceTag.value;
        }

        String getDeviceTagId() {
            return mDeviceTag.deviceTagId;
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
    private final PublishSubject<TagEvent> mTagEventSubject = PublishSubject.create();


    DeviceTagCollection(DeviceModel deviceModel) {
        mDeviceModel = deviceModel;
        mAferoClient = deviceModel.getAferoClient();
    }

    Observable<TagEvent> getTagEventObservable() {
        return mTagEventSubject;
    }

    /**
     * Adds a tag key/value to the collection persistently via the Afero Cloud. The tag is removed
     * if the device is disassociated from the account.
     *
     * @param key String specifying a unique identifier for the new tag
     * @param value String containing arbitrary value for the new tag
     * @return Observable that emits the new {@link Tag}
     */
    Observable<Tag> saveTag(String key, String value) {

        Tag tag = mTags.get(key);
        Observable<DeviceTag> tagObservable;

        if (tag == null) {
            tagObservable = mAferoClient.postDeviceTag(mDeviceModel.getId(), key, value);
        } else {
            tagObservable = mAferoClient.putDeviceTag(mDeviceModel.getId(), tag.getDeviceTagId(), key, value);
        }

        return tagObservable.flatMap(
                new Func1<DeviceTag, Observable<Tag>>() {
                    @Override
                    public Observable<Tag> call(DeviceTag deviceTag) {
                        return Observable.just(addTag(deviceTag));
                    }
                });
    }

    /**
     * Adds a temporary key/value tag to the collection.
     * The tag does *not* persist across sessions.
     * Replacing an existing persistent tag with this method,
     * also only lasts for the current session.
     *
     * @param key String specifying a unique identifier for the new tag
     * @param value String containing arbitrary value for the new tag
     * @see #saveTag(String, String)
     */
    void putTag(String key, String value) {
        mTags.put(key, new Tag(key, value));
    }

    /**
     * Deletes a tag from both local and persistent cloud storage.
     *
     * @param tag {@link Tag} to be deleted
     */
    Tag deleteTag(Tag tag) {
        if (tag.getDeviceTagId() != null) {
            mAferoClient.deleteDeviceTag(mDeviceModel.getId(), tag.getDeviceTagId())
                    .subscribe(new RxUtils.IgnoreResponseObserver<Void>());
        }
        return tag;
    }

    /**
     * Deletes a tag from both local and persistent cloud storage.
     *
     * @param key Unique indentifier of the tag.
     * @return {@link Tag} object that was removed; null if no such tag was found.
     */
    Tag deleteTag(String key) {
        Tag tag = mTags.remove(key);
        if (tag != null && tag.getDeviceTagId() != null) {
            mAferoClient.deleteDeviceTag(mDeviceModel.getId(), tag.getDeviceTagId())
                    .subscribe(new RxUtils.IgnoreResponseObserver<Void>());
        }
        return tag;
    }

    /**
     * Retrieves the value of a tag
     *
     * @param key Unique indentifier of the tag.
     * @return String containing the value of the tag.
     * @see #saveTag(String, String)
     * @see #putTag(String, String)
     */
    String getTag(String key) {
        Tag tag = mTags.get(key);
        return tag != null ? tag.getValue() : null;
    }

    /**
     * @return {@link Iterable} for the collection of tags currently attached to the device.
     */
    public Iterable<Tag> getTags() {
        return mTags.values();
    }

    /**
     * Setter for DeviceTags during DeviceModel JSON deserialization
     *
     * @param deviceTags Array of {@link DeviceTag} objects attached to this DeviceModel
     */
    void setDeviceTags(DeviceTag[] deviceTags) {
        mTags.clear();

        for (DeviceTag dt : deviceTags) {
            Tag tag = new Tag(dt);
            mTags.put(tag.getKey(), tag);
        }
    }

    void invalidateTag(String deviceTagAction, DeviceTag deviceTag) {
        try {
            TagAction action = TagAction.valueOf(deviceTagAction.toUpperCase(Locale.ROOT));
            switch (action) {
                case ADD:
                    addTag(deviceTag);
                    break;
                case UPDATE:
                    updateTag(deviceTag);
                    break;
                case DELETE:
                    deleteTagById(deviceTag.deviceTagId);
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

    Tag addTag(DeviceTag deviceTag) {
        Tag newTag = new Tag(deviceTag);

        // store the new tag in the local collection
        mTags.put(newTag.getKey(), newTag);

        mTagEventSubject.onNext(new TagEvent(TagAction.ADD, newTag));

        return newTag;
    }

    private Tag updateTag(DeviceTag deviceTag) {
        Tag tag = new Tag(deviceTag);

        // store the new tag in the local collection
        mTags.put(tag.getKey(), tag);

        mTagEventSubject.onNext(new TagEvent(TagAction.UPDATE, tag));

        return tag;
    }

    private Tag deleteTagById(String deviceTagId) {
        Tag tag = getTagById(deviceTagId);
        if (tag != null) {
            tag = mTags.remove(tag.getKey());
        }

        mTagEventSubject.onNext(new TagEvent(TagAction.DELETE, tag));

        return tag;
    }

}
