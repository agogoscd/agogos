package com.redhat.cpaas.k8s.client;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
public class TektonResourceClient {
    @Inject
    TektonClient tektonClient;

    public Pipeline getPipelineByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        PipelineList pipelineList = tektonClient.v1beta1().pipelines().list(options);

        if (pipelineList.getItems().isEmpty() || pipelineList.getItems().size() > 1) {
            return null;
        }

        return pipelineList.getItems().get(0);
    }

}
