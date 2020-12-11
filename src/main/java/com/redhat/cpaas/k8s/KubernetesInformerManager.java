package com.redhat.cpaas.k8s;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.cpaas.k8s.controllers.PipelineRunEventSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunList;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class KubernetesInformerManager {
    @ConfigProperty(name = "kubernetes.namespace")
    String namespace;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    PipelineRunEventSource pipelineRunEventSource;

    void onStart(@Observes StartupEvent ev) {
        OperationContext operationContext = new OperationContext().withNamespace(namespace);

        SharedInformerFactory sharedInformerFactory = kubernetesClient.informers();

        SharedIndexInformer<PipelineRun> pipelineRunEventInformer = sharedInformerFactory
                .sharedIndexInformerFor(PipelineRun.class, PipelineRunList.class, operationContext, 60 * 1000L);

        pipelineRunEventInformer.addEventHandler(pipelineRunEventSource);

        sharedInformerFactory.startAllRegisteredInformers();
    }
}
