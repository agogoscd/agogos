package com.redhat.cpaas.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.v1alpha1.ComponentGroupResource;
import com.redhat.cpaas.v1alpha1.ComponentGroupResourceList;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RegisterForReflection
public class ComponentGroupResourceClient {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentGroupResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    NonNamespaceOperation<ComponentGroupResource, ComponentGroupResourceList, Resource<ComponentGroupResource>> componentGroupClient;

    @PostConstruct
    void init() {
        componentGroupClient = kubernetesClient.customResources(ComponentGroupResource.class,
                ComponentGroupResourceList.class);
    }

    /**
     * Find the {@link ComponentGroupResource} by name.
     * 
     * @param name Name of the ComponentGroup.
     * @return The {@link ComponentGroupResource} or <code>null</code> in case it
     *         cannot be found
     */
    public ComponentGroupResource getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        ComponentGroupResourceList componentGroupResources = componentGroupClient.list(options);

        if (componentGroupResources.getItems().isEmpty() || componentGroupResources.getItems().size() > 1) {
            LOG.debug("ComponentGroup '{}' cannot be found", name);
            return null;
        }

        return componentGroupResources.getItems().get(0);
    }
}
