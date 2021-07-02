package com.redhat.agogos.cli;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.test.KubernetesTestServerSetup;
import com.redhat.agogos.test.ResourceUtils;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.scanner.ScannerException;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
public class ResourceLoaderTest {
    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    ResourceLoader resourceLoader;

    @Test
    @DisplayName("Should handle invalid content")
    void shouldHandleInvalidYaml() throws Exception {
        Exception ex = assertThrows(ApplicationException.class, () -> {
            resourceLoader.installKubernetesResources(
                    ResourceUtils.testResourceAsInputStream("loader/invalid.yaml"), "namespace");
        });

        assertEquals("Could not load resources", ex.getMessage());
        assertEquals(ScannerException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("Should be able to read arrays of JSON objects")
    void shouldHandleJSONArray() throws Exception {
        List<HasMetadata> installed = resourceLoader.installKubernetesResources(
                ResourceUtils.testResourceAsInputStream("loader/resources.json"), "namespace");

        assertEquals(2, installed.size());
    }

    @Test
    @DisplayName("Should be able to read a single JSON object")
    void shouldHandleJSONObject() throws Exception {
        List<HasMetadata> installed = resourceLoader.installKubernetesResources(
                ResourceUtils.testResourceAsInputStream("loader/single-resource.json"), "namespace");

        assertEquals(1, installed.size());
    }

    /**
     * @see https://yaml.org/spec/current.html#document%20boundary%20marker/
     */
    @Test
    @DisplayName("Should be able to read document with boundary markers")
    void shouldHandleBoundaryMarkers() throws Exception {
        List<HasMetadata> installed = resourceLoader.installKubernetesResources(
                ResourceUtils.testResourceAsInputStream("loader/resources-with-boundary-markers.yaml"), "namespace");

        assertEquals(2, installed.size());
    }

    @Test
    @DisplayName("Should not fail on empty list")
    void shouldNotFailOnEmptyList() throws Exception {
        List<HasMetadata> installed = resourceLoader.installKubernetesResources(Collections.emptyList(), "namespace");

        assertEquals(0, installed.size());
    }

    @Test
    @DisplayName("Should install a Component")
    void shouldInstallComponent() throws Exception {
        Component component = new Component();
        component.getMetadata().setName("component1");

        List<HasMetadata> installed = resourceLoader.installKubernetesResources(Arrays.asList(component), "default");

        assertEquals(1, installed.size());
    }
}
