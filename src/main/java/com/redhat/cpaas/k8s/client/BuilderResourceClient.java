package com.redhat.cpaas.k8s.client;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cpaas.k8s.model.BuilderResource;
import com.redhat.cpaas.k8s.model.BuilderResourceList;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.fabric8.tekton.client.TektonClient;

@ApplicationScoped
public class BuilderResourceClient {
    @Inject
    KubernetesClient kubernetesApiClient;

    @Inject
    TektonClient tektonApiClient;

    MixedOperation<BuilderResource, BuilderResourceList, Resource<BuilderResource>> builderResourceClient;

    @PostConstruct
    void init() {
        final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                .withName("builders.cpaas.redhat.com").withGroup("cpaas.redhat.com").withScope("Namespaced")
                .withVersion("v1alpha1").withPlural("builders").build();

        builderResourceClient = kubernetesApiClient.customResources(context, BuilderResource.class,
                BuilderResourceList.class);
    }

    public List<BuilderResource> list() {
        return builderResourceClient.list().getItems();
    }

    public BuilderResource create(final BuilderResource builder) {
        return builderResourceClient.createOrReplace(builder);
    }

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
