/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JSONUtils {

    private static ObjectMapper sObjectMapper;

    public static ObjectMapper getObjectMapper() {
        if (sObjectMapper == null) {
            sObjectMapper = new ObjectMapper();
            sObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        return sObjectMapper;
    }

    public static <T> T readValue(String src, Class<T> valueType)
            throws IOException, JsonParseException, JsonMappingException {
        return getObjectMapper().readValue(src, valueType);
    }

    public static <T> T readValue(byte[] src, Class<T> valueType)
            throws IOException, JsonParseException, JsonMappingException {
        return getObjectMapper().readValue(src, valueType);
    }

    public static String writeValueAsString(Object value)
            throws JsonProcessingException {
        return getObjectMapper().writeValueAsString(value);
    }

    public static String writeValueAsPrettyString(Object value)
            throws JsonProcessingException {
        return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

}
