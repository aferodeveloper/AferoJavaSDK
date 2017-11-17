/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import java.util.Collections;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.Callable;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class DeviceTagCollection {

    /**
     * This class represents a collection of key/value object "tag" that can be attached
     * to a {@link DeviceModel}.
     */
    public static class Tag implements Comparable<Tag> {

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

        public boolean hasKey() {
            return getKey() != null;
        }

        public String getValue() {
            return mDeviceTag.value;
        }

        public String getId() {
            return mDeviceTag.deviceTagId;
        }


        @Override
        public int compareTo(Tag thatTag) {
            final String thisKey = this.getKey();
            final String thatKey = thatTag.getKey();

            if (thisKey == null) {
                return thatKey == null ? 0 : -1;
            } else if (thatKey == null) {
                return 1;
            }

            return thisKey.compareTo(thatKey);
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

    private final Vector<Tag> mTags = new Vector<>();

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
     * @param key   String specifying a unique identifier for the new tag
     * @param value String containing arbitrary value for the new tag
     * @return Observable that emits the new {@link Tag}
     */
    Observable<Tag> saveTag(String key, String value) {

        Tag tag = getTagInternal(key);
        Observable<DeviceTag> tagObservable = tag != null
                ? mAferoClient.putDeviceTag(mDeviceModel.getId(), tag.getId(), key, value)
                : mAferoClient.postDeviceTag(mDeviceModel.getId(), key, value);

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
     * @param key   String specifying a unique identifier for the new tag
     * @param value String containing arbitrary value for the new tag
     * @see #saveTag(String, String)
     */
    void putTag(String key, String value) {
        addTagInternal(new Tag(key, value));
    }

    /**
     * Deletes a tag from both local and persistent cloud storage.
     *
     * @param tag {@link Tag} to be deleted
     */
    private Observable<Tag> deleteTag(Tag tag) {

        final Observable<Tag> removeLocalTag = Observable.fromCallable(
                new Callable<Tag>() {
                    Tag mTag;

                    Callable<Tag> init(Tag tag) {
                        mTag = tag;
                        return this;
                    }

                    @Override
                    public Tag call() throws Exception {
                        mTags.remove(mTag);
                        return mTag;
                    }
                }.init(tag));

        if (tag.getId() != null) {
            return mAferoClient.deleteDeviceTag(mDeviceModel.getId(), tag.getId())
                    .flatMap(new RxUtils.FlatMapper<Void, Tag>(removeLocalTag));
        }

        return removeLocalTag;
    }

    /**
     * Deletes a tag from both local and persistent cloud storage.
     *
     * @param key Unique indentifier of the tag.
     * @return {@link Tag} object that was removed; null if no such tag was found.
     */
    Observable<Tag> deleteTag(String key) {
        Tag tag = getTagInternal(key);
        if (tag != null) {
            return deleteTag(tag);
        }

        return Observable.error(new IllegalArgumentException("Not tag with key '" + key + "' found"));
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
        Tag tag = getTagInternal(key);
        return tag != null ? tag.getValue() : null;
    }

    /**
     * @return {@link Iterable} for the collection of tags currently attached to the device.
     */
    public Iterable<Tag> getTags() {
        return mTags;
    }

    /**
     * Setter for DeviceTags during DeviceModel JSON deserialization
     *
     * @param deviceTags Array of {@link DeviceTag} objects attached to this DeviceModel
     */
    void setDeviceTags(DeviceTag[] deviceTags) {
        mTags.clear();

        for (DeviceTag dt : deviceTags) {
            addTagInternal(new Tag(dt));
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
        int tagIndex = Collections.binarySearch(mTags, new Tag(key, null));
        if (tagIndex >= 0) {
            return mTags.get(tagIndex);
        }

        return null;
    }

    Tag getTagById(String deviceTagId) {

        for (Tag tag : mTags) {
            if (tag.getId() != null && tag.getId().equals(deviceTagId)) {
                return tag;
            }
        }

        return null;
    }

    Tag addTag(DeviceTag deviceTag) {
        Tag newTag = new Tag(deviceTag);

        addTagInternal(newTag);

        mTagEventSubject.onNext(new TagEvent(TagAction.ADD, newTag));

        return newTag;
    }

    private Tag addTagInternal(Tag tag) {

        int tagIndex = Collections.binarySearch(mTags, tag);
        if (tagIndex < 0) {
            tagIndex = -tagIndex - 1;
        } else {
            mTags.remove(tagIndex);
        }

        mTags.add(tagIndex, tag);

        return tag;
    }

    private Tag updateTag(DeviceTag deviceTag) {
        Tag tag = new Tag(deviceTag);

        addTagInternal(tag);

        mTagEventSubject.onNext(new TagEvent(TagAction.UPDATE, tag));

        return tag;
    }

    private Tag deleteTagById(String deviceTagId) {
        Tag tag = getTagById(deviceTagId);
        if (tag != null) {
            mTags.remove(tag);
        }

        mTagEventSubject.onNext(new TagEvent(TagAction.DELETE, tag));

        return tag;
    }

}
