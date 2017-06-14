/*
 * Copyright (c) 2014-2016 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceProfile;

import static org.junit.Assert.assertNotNull;

public class AferoTest {

    protected ObjectMapper mObjectMapper = new ObjectMapper();

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
