package com.redhat.cpaas.k8s.client;

import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentBuildResourceList;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build CR client.
 * 
 * This bean is used to interact with the {@link ComponentBuildResource} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
@RegisterForReflection
public class ComponentBuildResourceClient {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentBuildResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    MixedOperation<ComponentBuildResource, ComponentBuildResourceList, Resource<ComponentBuildResource>> componentBuildClient;

    @PostConstruct
    void init() {
        componentBuildClient = kubernetesClient.customResources(ComponentBuildResource.class,
                ComponentBuildResourceList.class);
    }

    public List<ComponentBuildResource> findByLabel(String namespace, String label, String value) {
        return componentBuildClient.inNamespace(namespace).withLabel(label, value).list().getItems();
    }

    public List<ComponentBuildResource> findByLabel(String namespace, String label) {
        return componentBuildClient.inNamespace(namespace).withLabel(label).list().getItems();
    }

    /**
     * Find the {@link ComponentBuildResource} by name.
     * 
     * @param name Name of the ComponentBuild.
     * @return The {@link ComponentBuildResource} or <code>null</code> in case it
     *         cannot be found
     */
    public ComponentBuildResource getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        ComponentBuildResourceList buildResources = componentBuildClient.list(options);

        if (buildResources.getItems().isEmpty() || buildResources.getItems().size() > 1) {
            LOG.debug("ComponentBuild '{}' cannot be found", name);
            return null;
        }

        return buildResources.getItems().get(0);
    }

    public ComponentBuildResource create(String name, String namespace) {

        ComponentBuildResource build = new ComponentBuildResource();

        build.getMetadata().setGenerateName(name + "-");
        build.getSpec().setComponent(name);

        // TODO: Add exception handling
        return componentBuildClient.inNamespace(namespace).create(build);
    }

    public ComponentBuildResource create(ComponentBuildResource build) {
        // TODO: Add exception handling
        return componentBuildClient.create(build);
    }
}
