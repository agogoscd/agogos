package com.redhat.agogos.core;

import com.redhat.agogos.core.retries.KubernetesClientRetries;
import io.fabric8.kubernetes.api.model.APIGroupList;
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
    KubernetesClientRetries retriesClient;

    public KubernetesFacade() {
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

    public APIGroupList getApiGroups() {
        return kubernetesClient.getApiGroups();
    }

    public APIResourceList getApiResources(String groupVersion) {
        return kubernetesClient.getApiResources(groupVersion);
    }

    public <T extends HasMetadata> T create(T resource) {
        return retriesClient.create(resource);
    }

    public <T extends HasMetadata> T create(T resource, Integer retries, Integer interval) {
        return retriesClient.create(resource, retries, interval);
    }

    public <T extends HasMetadata> T serverSideApply(T resource) {
        return retriesClient.serverSideApply(resource);
    }

    public <T extends HasMetadata> T serverSideApply(T resource, Integer retries, Integer interval) {
        return retriesClient.serverSideApply(resource, retries, interval);
    }

    public <T extends HasMetadata> T forceServerSideApply(T resource) {
        return retriesClient.forceServerSideApply(resource);
    }

    public <T extends HasMetadata> T forceServerSideApply(T resource, Integer retries, Integer interval) {
        return retriesClient.forceServerSideApply(resource, retries, interval);
    }

    public <T extends HasMetadata> T patchStatus(T resource) {
        return retriesClient.patchStatus(resource);
    }

    public <T extends HasMetadata> T patchStatus(T resource, Integer retries, Integer interval) {
        return retriesClient.patchStatus(resource, retries, interval);
    }

    public <T extends HasMetadata> T update(T resource) {
        return retriesClient.update(resource);
    }

    public <T extends HasMetadata> T update(T resource, Integer retries, Integer interval) {
        return retriesClient.update(resource, retries, interval);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String namespace, String name) {
        return retriesClient.delete(clazz, namespace, name);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String namespace, String name,
            Integer retries, Integer interval) {
        return retriesClient.delete(clazz, namespace, name, retries, interval);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String name) {
        return delete(clazz, null, name);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String name, Integer retries, Integer interval) {
        return delete(clazz, null, name, retries, interval);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(T resource) {
        return retriesClient.delete(resource);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(T resource, Integer retries, Integer interval) {
        return retriesClient.delete(resource, retries, interval);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name) {
        return retriesClient.get(clazz, namespace, name, false);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name, Integer retries, Integer interval) {
        return get(clazz, namespace, name, retries, interval, false);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String name) {
        return get(clazz, null, name);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String name, Integer retries, Integer interval) {
        return get(clazz, null, name, retries, interval);
    }

    private <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name, boolean retryOnNull) {
        return retriesClient.get(clazz, namespace, name, retryOnNull);
    }

    private <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name, Integer retries,
            Integer interval, boolean retryOnNull) {
        return retriesClient.get(clazz, namespace, name, retries, interval, retryOnNull);
    }

    /*
     * The getNonNull methods will additionally retry if the return value is null. This is
     * used in cases where we are waiting for a resource to be created.
     */
    public <T extends HasMetadata> T getNonNull(Class<T> clazz, String name) {
        return getNonNull(clazz, null, name);
    }

    public <T extends HasMetadata> T getNonNull(Class<T> clazz, String name, Integer retries, Integer interval) {
        return getNonNull(clazz, null, name, retries, interval);
    }

    public <T extends HasMetadata> T getNonNull(Class<T> clazz, String namespace, String name) {
        return get(clazz, namespace, name, true);
    }

    public <T extends HasMetadata> T getNonNull(Class<T> clazz, String namespace, String name, Integer retries,
            Integer interval) {
        return get(clazz, namespace, name, retries, interval, true);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz) {
        return list(clazz, getNamespace());
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, Integer retries, Integer interval) {
        return list(clazz, getNamespace(), new ListOptionsBuilder().build(), retries, interval);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace) {
        return list(clazz, namespace, new ListOptionsBuilder().build());
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, Integer retries, Integer interval) {
        return list(clazz, namespace, new ListOptionsBuilder().build(), retries, interval);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, ListOptions options) {
        return list(clazz, getNamespace(), options, false);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, ListOptions options, Integer retries, Integer interval) {
        return list(clazz, getNamespace(), options, retries, interval);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options) {
        return list(clazz, namespace, options, false);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options, Integer retries,
            Integer interval) {
        return list(clazz, namespace, options, retries, interval, false);
    }

    private <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options,
            Boolean retryOnEmptyList) {
        return retriesClient.list(clazz, namespace, options, retryOnEmptyList);
    }

    private <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options,
            Integer retries, Integer interval, Boolean retryOnEmptyList) {
        return retriesClient.list(clazz, namespace, options, retries, interval, retryOnEmptyList);
    }

    /*
     * The listNotEmpty methods will additionally retry if the list returned has a size of 0. This is
     * used in cases where we are waiting for a resource to be created.
     */
    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz) {
        return listNotEmpty(clazz, getNamespace(), new ListOptionsBuilder().build());
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, Integer retries, Integer interval) {
        return listNotEmpty(clazz, getNamespace(), new ListOptionsBuilder().build(), retries, interval);
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, String namespace) {
        return listNotEmpty(clazz, namespace, new ListOptionsBuilder().build());
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, String namespace, Integer retries, Integer interval) {
        return listNotEmpty(clazz, namespace, new ListOptionsBuilder().build(), retries, interval);
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, ListOptions options) {
        return listNotEmpty(clazz, getNamespace(), options);
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, ListOptions options, Integer retries,
            Integer interval) {
        return listNotEmpty(clazz, getNamespace(), options, retries, interval);
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, String namespace, ListOptions options) {
        return list(clazz, namespace, options, true);
    }

    public <T extends HasMetadata> List<T> listNotEmpty(Class<T> clazz, String namespace, ListOptions options, Integer retries,
            Integer interval) {
        return list(clazz, namespace, options, retries, interval, true);
    }

    public List<GenericKubernetesResource> getKubernetesResources(String namespace,
            String groupVersion, String kind, ListOptions options) {
        return retriesClient.getKubernetesResources(namespace, groupVersion, kind, options);
    }

    public List<GenericKubernetesResource> getKubernetesResources(String namespace,
            String groupVersion, String kind, ListOptions options, Integer retries, Integer interval) {
        return retriesClient.getKubernetesResources(namespace, groupVersion, kind, options, retries, interval);
    }

    public void waitForAllPodsRunning(String namespace) {
        retriesClient.waitForAllPodsRunning(namespace);
    }

    public void waitForAllPodsRunning(String namespace, Integer retries, Integer interval) {
        retriesClient.waitForAllPodsRunning(namespace, retries, interval);
    }

    public EventListener waitForEventListenerRunning(EventListener el) {
        return retriesClient.waitForEventListenerRunning(el);
    }

    public <T> T unmarshal(Class<T> clazz, InputStream input) {
        return kubernetesClient.getKubernetesSerialization().unmarshal(input, clazz);
    }

    public Deployment restartDeployment(String namespace, String name) {
        return retriesClient.restartDeployment(namespace, name);
    }
}
