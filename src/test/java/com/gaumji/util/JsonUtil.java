package com.gaumji.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T getData(String path, Class<T> type) {
        try (InputStream stream = ResourceLoader.getResource(path)) {
            return mapper.readValue(stream, type);
        } catch (Exception e) {
            log.error("Unable to read test data {}", path, e);
        }
        return null;
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Failed to parse JSON string", e);
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }
}
