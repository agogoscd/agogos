package com.redhat.agogos.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentList;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@RegisterForReflection
public class ComponentClient {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<Component, ComponentList, Resource<Component>> componentClient;

    @PostConstruct
    void init() {
        componentClient = kubernetesClient.customResources(Component.class, ComponentList.class);
    }

    /**
     * Find the {@link Component} by name.
     * 
     * @param name Name of the Component.
     * @return The {@link Component} or <code>null</code> in case it cannot
     *         be found
     */
    public Component getByName(String name, String namespace) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        ComponentList componentResources = componentClient.inNamespace(namespace).list(options);

        if (componentResources.getItems().isEmpty() || componentResources.getItems().size() > 1) {
            LOG.debug("Component '{}' cannot be found in the '{}' namespace", name, namespace);
            return null;
        }

        return componentResources.getItems().get(0);
    }
}
