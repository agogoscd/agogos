package com.redhat.agogos.webhooks.test;

import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class TestResources {

    @Inject
    static KubernetesSerialization mapper;

    /**
     * Reads test resource file and returns it as a String.
     */
    public static String asString(String path) throws IOException {
        return Files.readString(Paths.get("src", "test", "resources", path));
    }

    /**
     * Reads test resource JSON file and returns it as a Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> asMap(String path) throws IOException {
        return mapper.convertValue(asString(path), Map.class);
    }
}
