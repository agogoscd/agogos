package com.redhat.agogos.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.test.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.util.List;

@QuarkusTest
public class ResourceLoaderTest {
    @Inject
    ResourceLoader resourceLoader;

    @Test
    @DisplayName("Should handle invalid content")
    void shouldHandleInvalidYaml() throws Exception {
        Exception ex = Assertions.assertThrows(ApplicationException.class, () -> {
            resourceLoader.loadResources(ResourceUtils.testResourceAsInputStream("loader/invalid.yaml"));
        });

        assertEquals("Could not load resources", ex.getMessage());
        assertEquals(ScannerException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Should be able to read arrays of JSON objects")
    void shouldHandleJSONArray() throws Exception {
        List<HasMetadata> installed = resourceLoader.loadResources(
                ResourceUtils.testResourceAsInputStream("loader/resources.json"));

        assertEquals(2, installed.size());
    }

    @Test
    @DisplayName("Should be able to read a single JSON object")
    void shouldHandleJSONObject() throws Exception {
        List<HasMetadata> installed = resourceLoader.loadResources(
                ResourceUtils.testResourceAsInputStream("loader/single-resource.json"));

        assertEquals(1, installed.size());
    }

    /**
     * @see https://yaml.org/spec/current.html#document%20boundary%20marker/
     */
    @Test
    @DisplayName("Should be able to read document with boundary markers")
    void shouldHandleBoundaryMarkers() throws Exception {
        List<HasMetadata> installed = resourceLoader.loadResources(
                ResourceUtils.testResourceAsInputStream("loader/resources-with-boundary-markers.yaml"));

        assertEquals(2, installed.size());
    }
}
