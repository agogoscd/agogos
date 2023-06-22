package com.redhat.agogos;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KubernetesFacade {

    @Inject
    KubernetesClient kubernetesClient;

    public String getNamespace() {
        return kubernetesClient.getNamespace();
    }

    public <T extends HasMetadata> T create(T resource) {
        return kubernetesClient.resource(resource).create();
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String name) {
        return kubernetesClient.resources(clazz).inNamespace(getNamespace()).withName(name).get();
    }
}
