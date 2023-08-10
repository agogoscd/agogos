package com.redhat.agogos.core;

import com.redhat.agogos.core.retries.KubernetesClientRetries;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@QuarkusTest
public class KubernetesFacadeTest {

    @InjectMock
    KubernetesClientRetries kubernetesClientRetries;

    KubernetesSerialization kubernetesSerializationMock = Mockito.mock(new KubernetesSerialization().getClass());

    @InjectMock(convertScopes = true)
    KubernetesClient kubernetesClientMock;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Test
    public void getKubernetesClient() throws Exception {
        KubernetesClient kubeClient = kubernetesFacade.getKubernetesClient();

        Assertions.assertNotNull(kubeClient);
    }

    @Test
    public void getMasterUrl() throws Exception {
        kubernetesFacade.getMasterUrl();

        Mockito.verify(
                kubernetesClientMock,
                Mockito.times(1)).getMasterUrl();
    }

    @Test
    public void getNamespace() throws Exception {
        kubernetesFacade.getNamespace();

        Mockito.verify(
                kubernetesClientMock,
                Mockito.times(1)).getNamespace();
    }

    @Test
    public void getConfiguration() throws Exception {
        kubernetesFacade.getConfiguration();

        Mockito.verify(
                kubernetesClientMock,
                Mockito.times(1)).getConfiguration();
    }

    @Test
    public void getKubernetesVersion() throws Exception {
        kubernetesFacade.getKubernetesVersion();

        Mockito.verify(
                kubernetesClientMock,
                Mockito.times(1)).getKubernetesVersion();
    }

    @Test
    public void create() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.create(resource);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).create(resource);
    }

    @Test
    public void serverSideApply() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.serverSideApply(resource);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).serverSideApply(resource);
    }

    @Test
    public void update() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.update(resource);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).update(resource);
    }

    @Test
    public void deleteResource() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .withNewMetadata()
                .withName("resource")
                .withNamespace("default")
                .endMetadata()
                .build();
        kubernetesFacade.delete(resource);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).delete(
                        resource.getClass(),
                        resource.getMetadata().getNamespace(),
                        resource.getMetadata().getName());
    }

    @Test
    public void deleteClassAndName() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.delete(resource.getClass(), "resource");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).delete(
                        resource.getClass(),
                        null,
                        "resource");
    }

    @Test
    public void deleteClassNamespaceAndName() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.delete(resource.getClass(), "default", "resource");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).delete(
                        resource.getClass(),
                        "default",
                        "resource");
    }

    @Test
    public void getClassNamespaceAndName() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.get(resource.getClass(), "default", "resource");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        "default",
                        "resource");
    }

    @Test
    public void getClassAndName() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.get(resource.getClass(), "resource");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        null,
                        "resource");
    }

    @Test
    public void unmarshal() throws Exception {
        Mockito
                .when(kubernetesClientMock.getKubernetesSerialization())
                .thenReturn(kubernetesSerializationMock);
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        InputStream stream = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
        kubernetesFacade.unmarshal(resource.getClass(), stream);

        Mockito.verify(
                kubernetesSerializationMock,
                Mockito.times(1)).unmarshal(
                        stream, resource.getClass());
    }

    @Test
    public void list() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), "default");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", new ListOptionsBuilder().build(), false);
    }

    @Test
    public void listWithOptions() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), "default", options);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", options, false);
    }

    @Test
    public void waitForAllPodsRunning() throws Exception {
        kubernetesFacade.waitForAllPodsRunning("default");
        Mockito.verify(
                kubernetesClientRetries, Mockito.times(1)).waitForAllPodsRunning("default");
    }
}
