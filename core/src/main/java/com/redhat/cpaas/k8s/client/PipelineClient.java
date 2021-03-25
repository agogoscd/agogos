package com.redhat.cpaas.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.v1alpha1.PipelineResource;
import com.redhat.cpaas.v1alpha1.PipelineResourceList;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RegisterForReflection
public class PipelineClient {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<PipelineResource, PipelineResourceList, Resource<PipelineResource>> pipelineClient;

    @PostConstruct
    void init() {
        pipelineClient = kubernetesClient.customResources(PipelineResource.class, PipelineResourceList.class);
    }

    /**
     * Find the {@link PipelineResource} by name.
     * 
     * @param name Name of the Component.
     * @return The {@link PipelineResource} or <code>null</code> in case it cannot
     *         be found
     */
    public PipelineResource getByName(String name, String namespace) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        PipelineResourceList pipelineResources = pipelineClient.inNamespace(namespace).list(options);

        if (pipelineResources.getItems().isEmpty() || pipelineResources.getItems().size() > 1) {
            throw new MissingResourceException("Could not find Pipeline with name '{}' in namespace '{}'", name,
                    namespace);

        }

        return pipelineResources.getItems().get(0);
    }
}
