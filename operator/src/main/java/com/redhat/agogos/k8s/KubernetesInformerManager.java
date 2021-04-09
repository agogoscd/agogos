package com.redhat.agogos.k8s;

import com.redhat.agogos.k8s.event.PipelineRunEventHandler;
import com.redhat.agogos.v1alpha1.ComponentResource;
import com.redhat.agogos.v1alpha1.Pipeline;
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
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class KubernetesInformerManager {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    PipelineRunEventHandler eventHandler;

    SharedInformerFactory factory;

    private final long resyncPeriod = 60 * 1000L;

    void onStart(@Observes StartupEvent ev) {
        factory = kubernetesClient.informers();

        Map<String, String[]> labels = new HashMap<>(1);
        labels.put(Resource.RESOURCE.getLabel(),
                new String[] { HasMetadata.getKind(ComponentResource.class).toLowerCase(),
                        HasMetadata.getKind(Pipeline.class).toLowerCase() });

        SharedIndexInformer<PipelineRun> informer = factory.sharedIndexInformerFor(PipelineRun.class,
                new OperationContext().withLabelsIn(labels), resyncPeriod);

        informer.addEventHandler(eventHandler);

        factory.startAllRegisteredInformers();
    }

    void onStop(@Observes ShutdownEvent ev) {
        factory.stopAllRegisteredInformers();
    }
}
