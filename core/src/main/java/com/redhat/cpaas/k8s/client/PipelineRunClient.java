package com.redhat.cpaas.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.v1alpha1.PipelineRunResource;
import com.redhat.cpaas.v1alpha1.PipelineRunResourceList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
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
public class PipelineRunClient {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<PipelineRunResource, PipelineRunResourceList, Resource<PipelineRunResource>> pipelineRunClient;

    @PostConstruct
    void init() {
        pipelineRunClient = kubernetesClient.customResources(PipelineRunResource.class, PipelineRunResourceList.class);
    }

    public PipelineRunResource create(String name, String namespace) {
        PipelineRunResource pipelineRun = new PipelineRunResource();

        pipelineRun.setMetadata(new ObjectMetaBuilder().withGenerateName(name + "-").build());
        pipelineRun.getSpec().setPipeline(name);

        // TODO: Add exception handling
        return pipelineRunClient.inNamespace(namespace).create(pipelineRun);
    }

    public PipelineRunResource create(PipelineRunResource pipelineRun, String namespace) {
        return pipelineRunClient.inNamespace(namespace).create(pipelineRun);
    }

}
