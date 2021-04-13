package com.redhat.agogos.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.v1alpha1.Run;
import com.redhat.agogos.v1alpha1.RunList;
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
public class RunClient {
    private static final Logger LOG = LoggerFactory.getLogger(RunClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<Run, RunList, Resource<Run>> pipelineRunClient;

    @PostConstruct
    void init() {
        pipelineRunClient = kubernetesClient.customResources(Run.class, RunList.class);
    }

    public Run create(String name, String namespace) {
        Run pipelineRun = new Run();

        pipelineRun.setMetadata(new ObjectMetaBuilder().withGenerateName(name + "-").build());
        pipelineRun.getSpec().setPipeline(name);

        // TODO: Add exception handling
        return pipelineRunClient.inNamespace(namespace).create(pipelineRun);
    }

    public Run create(Run pipelineRun, String namespace) {
        return pipelineRunClient.inNamespace(namespace).create(pipelineRun);
    }

}
