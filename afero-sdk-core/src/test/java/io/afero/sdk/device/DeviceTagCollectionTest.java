/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.ResourceLoader;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class DeviceTagCollectionTest {

    private final static String KEY = "key";
    private final static String VALUE = "value";
    private final static String VALUE_1 = "value-1";
    private final static String VALUE_2 = "value-2";
    private final static String VALUE_3 = "value-3";
    private final static String VALUE_4 = "value-4";
    private final static String VALUE_5 = "value-5";
    private final static String DEVICE_TAG_ID = "device-tag-id";

    @Test
    public void addTag() throws Exception {
        makeTagTester()
                .addTag(KEY, VALUE)
                .verifyTag(KEY, VALUE)
                .verifyTagWasSavedToCloud(KEY, VALUE)
        ;
    }

    @Test
    public void addTagWithNullKey() throws Exception {
        makeTagTester()
                .addTag(null, VALUE)
                .verifyTag(null, VALUE)
                .verifyTagWasSavedToCloud(null, VALUE)
        ;
    }

    @Test
    public void updateTag() throws Exception {
        makeTagTester()
                .addTag(KEY, VALUE)
                .verifyTag(KEY, VALUE)

                .updateLastAddedTag(KEY, VALUE)
                .verifyTag(KEY, VALUE)
                .verifyTagWasSavedToCloud(KEY, VALUE)
        ;
    }

    @Test
    public void deleteTag() throws Exception {

        makeTagTester()
                .addTag(KEY, VALUE)
                .verifyTag(KEY, VALUE)
                .verifyTagWasSavedToCloud(KEY, VALUE)

                .deleteTag(KEY)
                .verifyTagWasDeletedLocally(KEY)
                .verifyTagWasDeletedFromCloud()
        ;
    }

    @Test
    public void getTag() throws Exception {
        makeTagTester()
                .addTag(KEY, VALUE_1)
                .addTag(KEY, VALUE_2)
                .addTag(KEY, VALUE_3)
                .addTag(KEY, VALUE_4)
                .addTag(KEY, VALUE_5)

                .getTags(KEY)
                .verifyGetTagReturnValue(new String[]{
                        VALUE_1,
                        VALUE_2,
                        VALUE_3,
                        VALUE_4,
                        VALUE_5
                })

                .getTags("bogus")
                .verifyGetTagReturnValueIsEmpty()
        ;
    }

    @Test
    public void getTags() throws Exception {
        makeTagTester()
                .addTag("key-1", VALUE)
                .addTag("key-2", VALUE)
                .addTag("key-3", VALUE)
                .addTag("key-4", VALUE)
                .addTag("key-5", VALUE)

                .verifyGetTagsCount(5)
        ;
    }

    @Test
    public void setDeviceTags() throws Exception {
        makeTagTester()
                .setDeviceTagsFromResource()
                .verifyTagsLoadedFromResource()
        ;
    }

    @Test
    public void invalidateTagAdd() throws Exception {
        makeTagTester()
                .invalidateTagAdd(DEVICE_TAG_ID, KEY, VALUE)
                .verifyTagWithId(DEVICE_TAG_ID, KEY, VALUE)
        ;
    }

    @Test
    public void invalidateTagUpdate() throws Exception {
        makeTagTester()
                .addDeviceTag(DEVICE_TAG_ID, KEY, VALUE)
                .invalidateTagUpdate(DEVICE_TAG_ID, KEY, "new-value")
                .verifyTagWithId(DEVICE_TAG_ID, KEY, "new-value")
        ;
    }

    @Test
    public void invalidateTagDelete() throws Exception {
        makeTagTester()
                .addDeviceTag(DEVICE_TAG_ID, KEY, VALUE)
                .invalidateTagDelete(DEVICE_TAG_ID)
                .verifyNoTagWithId(DEVICE_TAG_ID)
        ;
    }


    private TagTester makeTagTester() throws IOException {
        return new TagTester();
    }

    private class TagTester {

        final static String PATH_PREFIX = "deviceTagCollection/";

        final ResourceLoader resourceLoader = new ResourceLoader(PATH_PREFIX);
        final MockAferoClient aferoClient = new MockAferoClient(PATH_PREFIX);
        final DeviceTagCollection deviceTagCollection;
        final DeviceTag[] deviceTags;

        DeviceTagCollection.Tag deletedTag;
        DeviceTagCollection.Tag addedTag;
        Iterable<DeviceTagCollection.Tag> getTagResult;

        TagTester() throws IOException {
            DeviceProfile deviceProfile = resourceLoader.createObjectFromJSONResource("deviceProfile.json", DeviceProfile.class);
            DeviceModel deviceModel = new DeviceModel("device-id-1", deviceProfile, false, aferoClient);
            deviceTags = resourceLoader.createObjectFromJSONResource("deviceTags.json", DeviceTag[].class);
            deviceTagCollection = new DeviceTagCollection(deviceModel);
        }

        TagTester addTag(String key, String value) {
            deviceTagCollection.addTag(key, value).subscribe(
                    new Observer<DeviceTagCollection.Tag>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            assertTrue(false);
                        }

                        @Override
                        public void onNext(DeviceTagCollection.Tag tag) {
                            addedTag = tag;
                        }
                    });
            return this;
        }

        TagTester deleteTag(String key) {
            deviceTagCollection.removeTag(deviceTagCollection.getTags(key).iterator().next()).subscribe(
                    new Observer<DeviceTagCollection.Tag>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(DeviceTagCollection.Tag tag) {
                            deletedTag = tag;
                        }
                    }
            );

            return this;
        }

        TagTester verifyTag(String key, String value) {
            assertEquals(value, deviceTagCollection.getTags(key).iterator().next().getValue());
            return this;
        }

        TagTester verifyTagWasDeletedLocally(String key) {
            assertFalse(deviceTagCollection.getTags(key).iterator().hasNext());
            assertNotNull(deletedTag);
            assertEquals(key, deletedTag.getKey());
            return this;
        }

        TagTester verifyTagWasSavedToCloud(final String key, String value) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagInternal(key);
            assertNotNull(tag);
            assertEquals(value, tag.getValue());

            DeviceTag deviceTag = aferoClient.getTagById(tag.getId());
            assertNotNull(deviceTag);
            assertEquals(tag.getKey(), deviceTag.key);
            assertEquals(tag.getValue(), deviceTag.value);

            return this;
        }

        TagTester verifyTagWasNotSaved(final String key, String value) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagInternal(key);
            assertNotNull(tag);
            assertEquals(value, tag.getValue());

            DeviceTag deviceTag = aferoClient.getTagById(tag.getId());
            assertNull(deviceTag);

            return this;
        }

        TagTester getTags(String key) {
            getTagResult = deviceTagCollection.getTags(key);
            return this;
        }

        TagTester verifyGetTagReturnValue(String[] values) {
            List<String> tagList = Arrays.asList(values);

            for (DeviceTagCollection.Tag tag : getTagResult) {
                assertTrue(tagList.contains(tag.getValue()));
            }

            return this;
        }

        TagTester verifyGetTagReturnValueIsEmpty() {
            assertFalse(getTagResult.iterator().hasNext());
            return this;
        }

        TagTester verifyTagWasDeletedFromCloud() {
            assertNotNull(deletedTag);
            DeviceTag deviceTag = aferoClient.getTagById(deletedTag.getId());
            assertNull(deviceTag);

            return null;
        }

        TagTester verifyGetTagsCount(int expectedCount) {
            int actualCount = 0;

            for (DeviceTagCollection.Tag tag : deviceTagCollection.getTags()) {
                actualCount++;
            }

            assertEquals(expectedCount, actualCount);

            return this;
        }

        TagTester setDeviceTagsFromResource() throws IOException {
            deviceTagCollection.setDeviceTags(deviceTags);
            return this;
        }

        TagTester verifyTagsLoadedFromResource() throws IOException {
            assertNotEquals(0, deviceTags.length);

            for (DeviceTag dt : deviceTags) {
                DeviceTagCollection.Tag tag = deviceTagCollection.getTagById(dt.deviceTagId);
                assertNotNull(tag);
            }

            return this;
        }

        TagTester invalidateTagAdd(String deviceTagId, String key, String value) throws JsonProcessingException {
            DeviceTag deviceTag = new DeviceTag(key, value);
            deviceTag.deviceTagId = deviceTagId;
            deviceTagCollection.invalidateTag(DeviceTagCollection.TagAction.ADD.toString(), deviceTag);
            return this;
        }

        TagTester invalidateTagUpdate(String deviceTagId, String key, String value) throws JsonProcessingException {
            DeviceTag deviceTag = new DeviceTag(key, value);
            deviceTag.deviceTagId = deviceTagId;
            deviceTagCollection.invalidateTag(DeviceTagCollection.TagAction.UPDATE.toString(), deviceTag);
            return this;
        }

        TagTester invalidateTagDelete(String deviceTagId) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagById(deviceTagId);
            deviceTagCollection.invalidateTag(DeviceTagCollection.TagAction.DELETE.toString(), tag.getDeviceTag());
            return this;
        }

        TagTester addDeviceTag(String deviceTagId, String key, String value) {
            DeviceTag deviceTag = new DeviceTag(key, value);
            deviceTag.deviceTagId = deviceTagId;
            deviceTagCollection.addTag(deviceTag);
            return this;
        }

        TagTester verifyNoTagWithId(String deviceTagId) {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagById(deviceTagId);
            assertNull(tag);
            return this;
        }

        TagTester verifyTagWithId(String deviceTagId, String key, String value) {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagById(deviceTagId);
            assertNotNull(tag);
            assertEquals(key, tag.getKey());
            assertEquals(value, tag.getValue());
            return this;
        }

        TagTester updateLastAddedTag(String key, String value) {
            DeviceTagCollection.Tag tag = addedTag;
            addedTag = null;

            tag.getDeviceTag().key = key;
            tag.getDeviceTag().value = value;

            deviceTagCollection.updateTag(tag);

            return this;
        }
    }
}