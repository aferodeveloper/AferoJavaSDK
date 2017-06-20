/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.mock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader {

    public ObjectMapper objectMapper = new ObjectMapper();

    public ResourceLoader() {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    public <T> T createObjectFromJSONResource(String path, Class<T> valueType) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        return objectMapper.readValue(is, valueType);
    }
}
