package com.redhat.cpaas.k8s.client;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cpaas.k8s.model.BuilderResource;
import com.redhat.cpaas.model.Builder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.fabric8.tekton.client.TektonClient;

@ApplicationScoped
public class BuilderResourceClient {
    @ConfigProperty(name = "kubernetes.namespace")
    String namespace;

    @Inject
    KubernetesClient kubernetesApiClient;

    @Inject
    TektonClient tektonApiClient;

    NonNamespaceOperation<BuilderResource, BuilderResourceList, BuilderResourceDoneable, Resource<BuilderResource, BuilderResourceDoneable>> builderResourceClient;

    BuilderResourceClient() {
        KubernetesDeserializer.registerCustomKind("cpaas.redhat.com/v1alpha1", BuilderResource.KIND,
                BuilderResource.class);
    }

    @PostConstruct
    void init() {
        final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                .withName("builders.cpaas.redhat.com").withGroup("cpaas.redhat.com").withScope("Namespaced")
                .withVersion("v1alpha1").withPlural("builders").build();

        builderResourceClient = kubernetesApiClient.customResources(context, BuilderResource.class,
                BuilderResourceList.class, BuilderResourceDoneable.class).inNamespace(namespace);
    }

    public List<Builder> list() {
        return builderResourceClient.list().getItems().stream().map(item -> new Builder(item))
                .collect(Collectors.toList());
    }

    public Builder create(final Builder builder) {
        return new Builder(builderResourceClient.createOrReplace(new BuilderResource(builder)));
    }

    public Builder getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        BuilderResourceList builderResources = builderResourceClient.list(options);

        if (builderResources.getItems().isEmpty() || builderResources.getItems().size() > 1) {
            return null;
        }

        return new Builder(builderResources.getItems().get(0));
    }
}
