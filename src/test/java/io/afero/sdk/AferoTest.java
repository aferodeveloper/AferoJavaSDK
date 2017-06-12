/*
 * Copyright (c) 2014-2016 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import android.app.Activity;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.robolectric.Robolectric;

import java.io.IOException;
import java.io.InputStream;

import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.conclave.models.DeviceSync;

import static org.junit.Assert.assertNotNull;

public class AferoTest {

    protected ObjectMapper mObjectMapper = new ObjectMapper();
    Activity mActivity;

    public Activity getActivity() {
        if (mActivity == null) {
            mActivity = Robolectric.buildActivity(Activity.class).create().get();
        }
        return mActivity;
    }

    public DeviceProfile loadDeviceProfile(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(is);
        return mObjectMapper.readValue(is, DeviceProfile.class);
    }

    public DeviceSync loadDeviceSync(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(is);
        return mObjectMapper.readValue(is, DeviceSync.class);
    }
}
