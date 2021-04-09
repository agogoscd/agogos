package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.BuilderResource;
import com.redhat.agogos.v1alpha1.BuilderResourceList;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Builder CR client.
 * 
 * This bean is used to interact with the {@link BuilderResource} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
@RegisterForReflection
public class BuilderClient {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    MixedOperation<BuilderResource, BuilderResourceList, Resource<BuilderResource>> builderClient;

    @PostConstruct
    void init() {
        builderClient = kubernetesClient.customResources(BuilderResource.class, BuilderResourceList.class);
    }

    /**
     * Finds {@link BuilderResource} by name and returns it.
     * 
     * @param name Name of the Builder
     * @return {@link BuilderResource} object.
     */
    public BuilderResource getByName(String name) {
        ListOptionsBuilder builder = new ListOptionsBuilder()
                .withFieldSelector(String.format("metadata.name=%s", name));

        BuilderResourceList builders = builderClient.list(builder.build());

        if (builders.getItems().isEmpty() || builders.getItems().size() > 1) {
            return null;
        }

        return builders.getItems().get(0);
    }

}
