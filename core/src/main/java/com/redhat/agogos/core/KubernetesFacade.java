package com.redhat.agogos.core;

import com.redhat.agogos.core.retries.KubernetesClientRetries;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
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

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name, boolean retryOnNull) {
        return retries.get(clazz, namespace, name, retryOnNull);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name) {
        return get(clazz, namespace, name, true);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String name) {
        return get(clazz, null, name);
    }

    public <T> T unmarshal(Class<T> clazz, InputStream input) {
        return kubernetesClient.getKubernetesSerialization().unmarshal(input, clazz);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz) {
        return list(clazz, getNamespace(), new ListOptionsBuilder().build());
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace) {
        return list(clazz, namespace, new ListOptionsBuilder().build());
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, ListOptions options) {
        return list(clazz, getNamespace(), options);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options) {
        return list(clazz, namespace, options, false);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options,
            boolean retryOnEmptyList) {
        return retries.list(clazz, namespace, options, retryOnEmptyList);
    }

    public void waitForAllPodsRunning(String namespace) {
        retries.waitForAllPodsRunning(namespace);
    }

    public EventListener waitForEventListenerRunning(String namespace, String name) {
        return retries.waitForEventListenerRunning(namespace, name);
    }
}
