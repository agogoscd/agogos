package com.redhat.cpaas.k8s.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.model.ComponentGroupResource;
import com.redhat.cpaas.k8s.model.ComponentGroupResourceList;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

@ApplicationScoped
public class ComponentGroupResourceClient {
    private static final Logger LOG = Logger.getLogger(ComponentGroupResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    NonNamespaceOperation<ComponentGroupResource, ComponentGroupResourceList, Resource<ComponentGroupResource>> componentGroupClient;

    @PostConstruct
    void init() {
        final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                .withName("groups.cpaas.redhat.com") //
                .withGroup("cpaas.redhat.com") //
                .withScope("Namespaced") //
                .withVersion("v1alpha1") //
                .withPlural("groups") //
                .build();

        componentGroupClient = kubernetesClient.customResources(context, ComponentGroupResource.class,
                ComponentGroupResourceList.class);
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
