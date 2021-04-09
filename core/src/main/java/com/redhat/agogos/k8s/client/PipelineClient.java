package com.redhat.agogos.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.PipelineList;
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

    MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pipelineClient;

    @PostConstruct
    void init() {
        pipelineClient = kubernetesClient.customResources(Pipeline.class, PipelineList.class);
    }

    /**
     * Find the {@link Pipeline} by name.
     * 
     * @param name Name of the Component.
     * @return The {@link Pipeline} or <code>null</code> in case it cannot
     *         be found
     */
    public Pipeline getByName(String name, String namespace) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        PipelineList pipelineResources = pipelineClient.inNamespace(namespace).list(options);

        if (pipelineResources.getItems().isEmpty() || pipelineResources.getItems().size() > 1) {
            throw new MissingResourceException("Could not find Pipeline with name '{}' in namespace '{}'", name,
                    namespace);

        }

        return pipelineResources.getItems().get(0);
    }
}
