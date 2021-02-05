package com.redhat.cpaas.k8s.client;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.ComponentResourceList;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@Singleton
public class ComponentResourceClient {
    private static final Logger LOG = Logger.getLogger(ComponentResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<ComponentResource, ComponentResourceList, Resource<ComponentResource>> componentClient;

    @PostConstruct
    void init() {
        componentClient = kubernetesClient.customResources(ComponentResource.class, ComponentResourceList.class);
    }

    /**
     * Find the {@link ComponentResource} by name.
     * 
     * @param name Name of the Component.
     * @return The {@link ComponentResource} or <code>null</code> in case it cannot
     *         be found
     */
    public ComponentResource getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        ComponentResourceList componentResources = componentClient.list(options);

        if (componentResources.getItems().isEmpty() || componentResources.getItems().size() > 1) {
            LOG.debugv("Component ''{0}'' cannot be found", name);
            return null;
        }

        return componentResources.getItems().get(0);
    }
}
