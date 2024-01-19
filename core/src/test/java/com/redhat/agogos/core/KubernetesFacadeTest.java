package com.redhat.agogos.core;

import com.redhat.agogos.core.retries.KubernetesClientRetries;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;
import jakarta.inject.Inject;
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

    @MockitoConfig(convertScopes = true)
    @InjectMock
    KubernetesClient kubernetesClientMock;

    @Inject
    KubernetesFacade kubernetesFacade;

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
                Mockito.times(1))
                .create(resource);
    }

    @Test
    public void createWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.create(resource, 9, 9);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).create(resource, 9, 9);
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
    public void serverSideApplyWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.serverSideApply(resource, 5, 5);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).serverSideApply(resource, 5, 5);
    }

    @Test
    public void forceServerSideApply() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.forceServerSideApply(resource);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).forceServerSideApply(resource);
    }

    @Test
    public void forceServerSideApplyWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.forceServerSideApply(resource, 5, 5);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).forceServerSideApply(resource, 5, 5);
    }

    @Test
    public void update() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.update(resource);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1))
                .update(resource);
    }

    @Test
    public void updateWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.update(resource, 7, 7);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).update(resource, 7, 7);
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
                        resource);
    }

    @Test
    public void deleteResourceWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .withNewMetadata()
                .withName("resource")
                .withNamespace("default")
                .endMetadata()
                .build();
        kubernetesFacade.delete(resource, 3, 3);
        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).delete(
                        resource,
                        3,
                        3);
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
    public void deleteClassAndNameWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.delete(resource.getClass(), "resource", 1, 1);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).delete(
                        resource.getClass(),
                        null,
                        "resource",
                        1,
                        1);
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
    public void deleteClassNamespaceAndNameWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.delete(resource.getClass(), "default", "resource", 11, 11);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).delete(
                        resource.getClass(),
                        "default",
                        "resource",
                        11,
                        11);
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
                        "resource",
                        false);
    }

    @Test
    public void getClassNamespaceAndNameWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.get(resource.getClass(), "default", "resource", 21, 21);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        "default",
                        "resource",
                        21,
                        21,
                        false);
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
                        "resource",
                        false);
    }

    @Test
    public void getClassAndNameWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.get(resource.getClass(), "resource", 22, 22);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        null,
                        "resource",
                        22,
                        22,
                        false);
    }

    @Test
    public void getNonNullClassNamespaceAndName() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.getNonNull(resource.getClass(), "default", "resource");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        "default",
                        "resource",
                        true);
    }

    @Test
    public void getNonNullClassNamespaceAndNameWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();
        kubernetesFacade.getNonNull(resource.getClass(), "default", "resource", 23, 23);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        "default",
                        "resource",
                        23,
                        23,
                        true);
    }

    @Test
    public void getNonNullClassAndName() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.getNonNull(resource.getClass(), "resource");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        null,
                        "resource",
                        true);
    }

    @Test
    public void getNonNullClassAndNameWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder()
                .build();
        kubernetesFacade.getNonNull(resource.getClass(), "resource", 24, 24);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).get(
                        resource.getClass(),
                        null,
                        "resource",
                        24,
                        24,
                        true);
    }

    @Test
    public void listClass() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass());

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, new ListOptionsBuilder().build(), false);
    }

    @Test
    public void listClassWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), 25, 25);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, new ListOptionsBuilder().build(), 25, 25, false);
    }

    @Test
    public void listClassAndNamespace() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), "default");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", new ListOptionsBuilder().build(), false);
    }

    @Test
    public void listClassAndNamespaceWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), "default", 25, 25);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", new ListOptionsBuilder().build(), 25, 25, false);
    }

    @Test
    public void listClassAndOptions() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), options);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, options, false);
    }

    @Test
    public void listClassAndOptionsWithRetries() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), options, 27, 27);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, options, 27, 27, false);
    }

    @Test
    public void listClassNamespaceAndOptions() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), "default", options);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", options, false);
    }

    @Test
    public void listClassNamespaceAndOptionsWithRetries() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.list(resource.getClass(), "default", options, 28, 28);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", options, 28, 28, false);
    }

    @Test
    public void listNotEmptyClass() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass());

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, new ListOptionsBuilder().build(), true);
    }

    @Test
    public void listNotEmptyClassWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), 30, 30);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, new ListOptionsBuilder().build(), 30, 30, true);
    }

    @Test
    public void listNotEmptyClassAndNamespace() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), "default");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", new ListOptionsBuilder().build(), true);
    }

    @Test
    public void listNotEmptyClassAndNamespaceWithRetries() throws Exception {
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), "default", 31, 31);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", new ListOptionsBuilder().build(), 31, 31, true);
    }

    @Test
    public void listNotEmptyClassAndOptions() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), options);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, options, true);
    }

    @Test
    public void listNotEmptyClassAndOptionsWithRetries() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), options, 32, 32);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), null, options, 32, 32, true);
    }

    @Test
    public void listNotEmptyClassNamespaceAndOptions() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), "default", options);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", options, true);
    }

    @Test
    public void listNotEmptyClassNamespaceAndOptionsWithRetries() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();
        GenericKubernetesResource resource = new GenericKubernetesResourceBuilder().build();

        kubernetesFacade.listNotEmpty(resource.getClass(), "default", options, 33, 33);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).list(
                        resource.getClass(), "default", options, 33, 33, true);
    }

    @Test
    public void kubernetesResourcesNamespaceGroupVersionKindAndOptions() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();

        kubernetesFacade.getKubernetesResources("default", "groupVersion", "kind", options);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).getKubernetesResources("default",
                        "groupVersion", "kind", options);
    }

    @Test
    public void kubernetesResourcesNamespaceGroupVersionKindAndOptionsWithRetries() throws Exception {
        ListOptions options = new ListOptionsBuilder().build();

        kubernetesFacade.getKubernetesResources("default", "groupVersion", "kind", options, 133, 133);

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).getKubernetesResources("default",
                        "groupVersion", "kind", options, 133, 133);
    }

    @Test
    public void waitForAllPodsRunning() throws Exception {
        kubernetesFacade.waitForAllPodsRunning("default");
        Mockito.verify(
                kubernetesClientRetries, Mockito.times(1)).waitForAllPodsRunning("default");
    }

    @Test
    public void waitForAllPodsRunningWithRetries() throws Exception {
        kubernetesFacade.waitForAllPodsRunning("default", 40, 40);
        Mockito.verify(
                kubernetesClientRetries, Mockito.times(1)).waitForAllPodsRunning("default", 40, 40);
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
    public void kubernetesRestartDeployment() throws Exception {
        kubernetesFacade.restartDeployment("default", "deployment");

        Mockito.verify(
                kubernetesClientRetries,
                Mockito.times(1)).restartDeployment("default", "deployment");
    }
}
