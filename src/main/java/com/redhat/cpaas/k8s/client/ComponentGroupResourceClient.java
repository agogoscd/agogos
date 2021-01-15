package com.redhat.cpaas.k8s.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.model.ComponentGroupResource;
import com.redhat.cpaas.k8s.model.ComponentGroupResourceDoneable;
import com.redhat.cpaas.k8s.model.ComponentGroupResourceList;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

@ApplicationScoped
public class ComponentGroupResourceClient {
    private static final Logger LOG = Logger.getLogger(ComponentGroupResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    NonNamespaceOperation<ComponentGroupResource, ComponentGroupResourceList, ComponentGroupResourceDoneable, Resource<ComponentGroupResource, ComponentGroupResourceDoneable>> componentGroupClient;

    @PostConstruct
    void init() {
        KubernetesDeserializer.registerCustomKind("cpaas.redhat.com/v1alpha1", ComponentGroupResource.KIND,
                ComponentGroupResource.class);

        final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                .withName("groups.cpaas.redhat.com") //
                .withGroup("cpaas.redhat.com") //
                .withScope("Namespaced") //
                .withVersion("v1alpha1") //
                .withPlural("groups") //
                .build();

        componentGroupClient = kubernetesClient.customResources(context, ComponentGroupResource.class,
                ComponentGroupResourceList.class, ComponentGroupResourceDoneable.class);
    }

    public ComponentGroupResource getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        ComponentGroupResourceList componentGroupResources = componentGroupClient.list(options);

        if (componentGroupResources.getItems().isEmpty() || componentGroupResources.getItems().size() > 1) {
            return null;
        }

        return componentGroupResources.getItems().get(0);
    }
}
