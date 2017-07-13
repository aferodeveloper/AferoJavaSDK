/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.mock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader {

    private ObjectMapper objectMapper = new ObjectMapper();
    private String pathPrefix = "";

    public ResourceLoader() {
        this("");
    }

    public ResourceLoader(String pathPrefix) {
        this.pathPrefix = pathPrefix;
        this.objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    public <T> T createObjectFromJSONResource(String path, Class<T> valueType) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(pathPrefix + path);
        return objectMapper.readValue(is, valueType);
    }
}
