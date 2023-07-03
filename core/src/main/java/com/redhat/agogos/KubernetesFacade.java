package com.redhat.agogos;

import com.redhat.agogos.retries.KubernetesClientRetries;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

@ApplicationScoped
public class KubernetesFacade {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    KubernetesClientRetries retries;

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public URL getMasterUrl() {
        return kubernetesClient.getMasterUrl();
    }

    public String getNamespace() {
        return kubernetesClient.getNamespace();
    }

    public Config getConfiguration() {
        return kubernetesClient.getConfiguration();
    }

    public VersionInfo getKubernetesVersion() {
        return kubernetesClient.getKubernetesVersion();
    }

    public <T extends HasMetadata> T create(T resource) {
        return retries.create(resource);
    }

    public <T extends HasMetadata> T serverSideApply(T resource) {
        return retries.serverSideApply(resource);
    }

    public <T extends HasMetadata> T update(T resource) {
        return retries.update(resource);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(
            Class<T> clazz,
            String namespace,
            String name) {
        return retries.delete(clazz, namespace, name);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String name) {
        return delete(clazz, null, name);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(T resource) {
        return delete(
                resource.getClass(),
                resource.getMetadata().getNamespace(),
                resource.getMetadata().getName());
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name) {
        return retries.get(clazz, namespace, name);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String name) {
        return get(clazz, null, name);
    }

    public <T extends HasMetadata> T unmarshal(Class<T> clazz, InputStream input) {
        return kubernetesClient.getKubernetesSerialization().unmarshal(input, clazz);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace) {
        return retries.list(clazz, namespace);
    }

    public void waitForAllPodsRunning(String namespace) {
        retries.waitForAllPodsRunning(namespace);
    }
}
