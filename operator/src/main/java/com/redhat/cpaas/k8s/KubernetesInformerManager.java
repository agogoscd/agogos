package com.redhat.cpaas.k8s;

import com.redhat.cpaas.k8s.event.PipelineRunEventSource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class KubernetesInformerManager {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    PipelineRunEventSource pipelineRunEventSource;

    SharedInformerFactory sharedInformerFactory;

    @PostConstruct
    void init() {
        sharedInformerFactory = kubernetesClient.informers();
    }

    void onStart(@Observes StartupEvent ev) {
        SharedIndexInformer<PipelineRun> pipelineRunEventInformer = sharedInformerFactory
                .sharedIndexInformerFor(PipelineRun.class, 60 * 1000L);

        pipelineRunEventInformer.addEventHandler(pipelineRunEventSource);

        sharedInformerFactory.startAllRegisteredInformers();
    }

    void onStop(@Observes ShutdownEvent ev) {
        sharedInformerFactory.stopAllRegisteredInformers();
    }
}
