/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.Test;

import java.io.IOException;

import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.ResourceLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class DeviceTagCollectionTest {

    private final static String KEY = "key";
    private final static String VALUE = "value";
    private final static String DEVICE_TAG_ID = "device-tag-id";

    @Test
    public void saveTag() throws Exception {
        makeTagTester()
                .saveTag(KEY, VALUE)
                .verifyTagWasSavedToCloud(KEY, VALUE)
        ;
    }

    @Test
    public void putTag() throws Exception {
        makeTagTester()
                .putTag(KEY, VALUE)
                .verifyTag(KEY, VALUE)
                .verifyTagWasNotSaved(KEY, VALUE)
        ;
    }

    @Test
    public void deleteTag() throws Exception {
        final String KEY_LOCAL = "key-local";
        final String KEY_CLOUD = "key-cloud";

        makeTagTester()
                .putTag(KEY_LOCAL, VALUE)
                .saveTag(KEY_CLOUD, VALUE)

                .verifyTag(KEY_LOCAL, VALUE)
                .verifyTagWasSavedToCloud(KEY_CLOUD, VALUE)

                .deleteTag(KEY_LOCAL)
                .verifyTagWasDeletedLocally(KEY_LOCAL)

                .deleteTag(KEY_CLOUD)
                .verifyTagWasDeletedLocally(KEY_CLOUD)
                .verifyTagWasDeletedFromCloud()
        ;
    }

    @Test
    public void getTag() throws Exception {
        makeTagTester()
                .putTag(KEY, VALUE)

                .getTag(KEY)
                .verifyGetTagReturnValue(VALUE)

                .getTag("bogus")
                .verifyGetTagReturnValue(null)
        ;
    }

    @Test
    public void getTags() throws Exception {
        makeTagTester()
                .putTag("key-1", VALUE)
                .putTag("key-2", VALUE)
                .putTag("key-3", VALUE)
                .putTag("key-4", VALUE)
                .putTag("key-5", VALUE)

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

        final static String PATH_PREFIX = "resources/deviceTagCollection/";

        final ResourceLoader resourceLoader = new ResourceLoader(PATH_PREFIX);
        final MockAferoClient aferoClient = new MockAferoClient(PATH_PREFIX);
        final DeviceTagCollection deviceTagCollection;
        final DeviceTag[] deviceTags;

        DeviceTagCollection.Tag deletedTag;
        String valueReturnedFromGetTag;

        TagTester() throws IOException {
            DeviceProfile deviceProfile = resourceLoader.createObjectFromJSONResource("deviceProfile.json", DeviceProfile.class);
            DeviceModel deviceModel = new DeviceModel("device-id-1", deviceProfile, false, aferoClient);
            deviceTags = resourceLoader.createObjectFromJSONResource("deviceTags.json", DeviceTag[].class);
            deviceTagCollection = new DeviceTagCollection(deviceModel);
        }

        TagTester saveTag(String key, String value) {
            deviceTagCollection.saveTag(key, value).subscribe();
            return this;
        }

        TagTester deleteTag(String key) {
            deletedTag = deviceTagCollection.deleteTag(key);
            return this;
        }

        TagTester putTag(String key, String value) {
            deviceTagCollection.putTag(key, value);
            return this;
        }

        TagTester verifyTag(String key, String value) {
            assertEquals(value, deviceTagCollection.getTag(key));
            return this;
        }

        TagTester verifyTagWasDeletedLocally(String key) {
            assertNull(deviceTagCollection.getTag(key));
            assertNotNull(deletedTag);
            assertEquals(key, deletedTag.getKey());
            return this;
        }

        TagTester verifyTagWasSavedToCloud(final String key, String value) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagInternal(key);
            assertNotNull(tag);
            assertEquals(value, tag.getValue());

            DeviceTag deviceTag = aferoClient.getTagById(tag.getDeviceTagId());
            assertNotNull(deviceTag);
            assertEquals(tag.getKey(), deviceTag.key);
            assertEquals(tag.getValue(), deviceTag.value);

            return this;
        }

        TagTester verifyTagWasNotSaved(final String key, String value) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagInternal(key);
            assertNotNull(tag);
            assertEquals(value, tag.getValue());

            DeviceTag deviceTag = aferoClient.getTagById(tag.getDeviceTagId());
            assertNull(deviceTag);

            return this;
        }

        TagTester getTag(String key) {
            valueReturnedFromGetTag = deviceTagCollection.getTag(key);
            return this;
        }

        TagTester verifyGetTagReturnValue(String expectedValue) {
            assertEquals(expectedValue, valueReturnedFromGetTag);
            return this;
        }

        TagTester verifyTagWasDeletedFromCloud() {
            assertNotNull(deletedTag);
            DeviceTag deviceTag = aferoClient.getTagById(deletedTag.getDeviceTagId());
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
            DeviceTag deviceTag = new DeviceTag(deviceTagId, key, value);
            deviceTagCollection.invalidateTag(DeviceTagCollection.TagAction.ADD.toString(), deviceTag);
            return this;
        }

        TagTester invalidateTagUpdate(String deviceTagId, String key, String value) throws JsonProcessingException {
            DeviceTag deviceTag = new DeviceTag(deviceTagId, key, value);
            deviceTagCollection.invalidateTag(DeviceTagCollection.TagAction.UPDATE.toString(), deviceTag);
            return this;
        }

        TagTester invalidateTagDelete(String deviceTagId) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceTagCollection.getTagById(deviceTagId);
            DeviceTag deviceTag = new DeviceTag(deviceTagId, tag.getKey(), tag.getValue());
            deviceTagCollection.invalidateTag(DeviceTagCollection.TagAction.DELETE.toString(), deviceTag);
            return this;
        }

        TagTester addDeviceTag(String deviceTagId, String key, String value) {
            DeviceTag deviceTag = new DeviceTag(deviceTagId, key, value);
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
    }
}