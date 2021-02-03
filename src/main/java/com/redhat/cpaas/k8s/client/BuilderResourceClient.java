package com.redhat.cpaas.k8s.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cpaas.k8s.model.BuilderResource;
import com.redhat.cpaas.k8s.model.BuilderResourceList;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;

/**
 * Builder CR client.
 * 
 * This bean is used to interact with the {@link BuilderResource} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
public class BuilderResourceClient {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    MixedOperation<BuilderResource, BuilderResourceList, Resource<BuilderResource>> builderResourceClient;

    @PostConstruct
    void init() {
        builderResourceClient = kubernetesClient.customResources(BuilderResource.class, BuilderResourceList.class);
    }

    /**
     * Finds {@link BuilderResource} by name and returns it.
     * 
     * @return {@link BuilderResource} object.
     */
    public BuilderResource getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        BuilderResourceList builderResources = builderResourceClient.list(options);

        if (builderResources.getItems().isEmpty() || builderResources.getItems().size() > 1) {
            return null;
        }

        return builderResources.getItems().get(0);
    }
}
