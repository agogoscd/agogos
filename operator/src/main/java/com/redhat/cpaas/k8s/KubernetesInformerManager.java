package com.redhat.cpaas.k8s;

import com.redhat.cpaas.k8s.event.PipelineRunEventHandler;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import com.redhat.cpaas.v1alpha1.PipelineResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class KubernetesInformerManager {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    PipelineRunEventHandler pipelineRunEventHandler;

    SharedInformerFactory sharedInformerFactory;

    private final long resyncPeriod = 60 * 1000L;

    @PostConstruct
    void init() {
        sharedInformerFactory = kubernetesClient.informers();
    }

    void onStart(@Observes StartupEvent ev) {
        Map<String, String[]> labels = new HashMap<>();

        labels.put(ResourceLabels.RESOURCE.getValue(),
                new String[] { HasMetadata.getKind(ComponentResource.class).toLowerCase(),
                        HasMetadata.getKind(PipelineResource.class).toLowerCase() });

        System.out.println(labels);

        SharedIndexInformer<PipelineRun> informer = sharedInformerFactory.sharedIndexInformerFor(PipelineRun.class,
                new OperationContext().withLabelsIn(labels), resyncPeriod);

        informer.addEventHandler(pipelineRunEventHandler);

        sharedInformerFactory.startAllRegisteredInformers();
    }

    void onStop(@Observes ShutdownEvent ev) {
        sharedInformerFactory.stopAllRegisteredInformers();
    }
}
