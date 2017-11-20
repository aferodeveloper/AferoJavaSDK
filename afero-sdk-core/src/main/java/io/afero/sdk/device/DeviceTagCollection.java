/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.Callable;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.utils.BinarySearch;
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

        private DeviceTag mDeviceTag;

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
        DELETE
    }

    public static class TagEvent {

        public final TagAction action;
        public final Tag tag;

        private TagEvent(TagAction a, Tag t) {
            action = a;
            tag = t;
        }
    }

    private static final Iterable<Tag> EMPTY_TAG_LIST = new ArrayList<>();

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
     * Adds a {@link Tag} key/value to the collection persistently via the Afero Cloud. The Tag is removed
     * if the device is disassociated from the account.
     *
     * @param key   String specifying the key used to search for the new Tag.
     * @param value String containing arbitrary value for the new Tag
     * @return {@link Observable} that emits the new Tag
     */
    Observable<Tag> addTag(String key, String value) {
        return mAferoClient.postDeviceTag(mDeviceModel.getId(), key, value)
                .flatMap(new Func1<DeviceTag, Observable<Tag>>() {
                    @Override
                    public Observable<Tag> call(DeviceTag deviceTag) {
                        return Observable.just(addTag(deviceTag));
                    }
                });
    }

    /**
     * Updates the key & values of the {@link Tag} specified by tagId.
     *
     * @param tagId String containing the unique identifier of a specific Tag.
     * @param key   String specifying the new key for the Tag.
     * @param value String specifying the new value for the Tag
     * @return {@link Observable} that emits the updated Tag
     */
    Observable<Tag> updateTag(String tagId, String key, String value) {
        return mAferoClient.putDeviceTag(mDeviceModel.getId(), tagId, key, value)
                .flatMap(new Func1<DeviceTag, Observable<Tag>>() {
                    @Override
                    public Observable<Tag> call(DeviceTag deviceTag) {
                        final Tag tag = getTagById(deviceTag.deviceTagId);
                        if (tag != null) {
                            tag.mDeviceTag = deviceTag;
                            mTagEventSubject.onNext(new TagEvent(TagAction.UPDATE, tag));
                            return Observable.just(tag);
                        }
                        return Observable.empty();
                    }
                });
    }

    /**
     * Permanently removes a tag from the collection.
     *
     * @param tag {@link Tag} to be deleted
     */
    Observable<Tag> removeTag(Tag tag) {

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
     * Retrieves all tags that match the specified key.
     *
     * @param key String specifying the key used to search for the tag. If null all tags are returned.
     * @return {@link Iterable} containing the tags that match the specified key, if any.
     */
    Iterable<Tag> getTags(String key) {
        final Tag tag = new Tag(key, null);
        int startIndex = BinarySearch.lowerBound(mTags, tag);

        if (startIndex < 0) {
            return EMPTY_TAG_LIST;
        }

        int endIndex = BinarySearch.upperBound(mTags, tag) + 1;

        return mTags.subList(startIndex, endIndex);
    }

    /**
     * @return {@link Iterable} for the collection of tags currently attached to the device.
     */
    public Iterable<Tag> getTags() {
        return mTags;
    }

    /**
     * @return true if at least one tag exists matching the specified key; false otherwise.
     */
    public boolean hasTag(String key) {
        return getTagInternal(key) != null;
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
        }

        mTags.add(tagIndex, tag);

        return tag;
    }

    private Tag updateTag(DeviceTag deviceTag) {
        Tag tag = new Tag(deviceTag);

        if (deviceTag.deviceTagId != null) {
            Tag oldTag = getTagById(deviceTag.deviceTagId);
            if (oldTag != null) {
                mTags.remove(oldTag);
            }
        }

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
