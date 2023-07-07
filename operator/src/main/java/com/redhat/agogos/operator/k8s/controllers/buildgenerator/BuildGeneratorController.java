package com.redhat.agogos.operator.k8s.controllers.buildgenerator;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.k8s.client.AgogosClient;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.operator.k8s.controllers.trigger.TriggerDependentResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

public class BuildGeneratorController implements Namespaced, Reconciler<CustomRun>, Cleaner<CustomRun> {
    /*
     * This Controller has one job, it creates Agogos resources from a Tekton Trigger that
     * has fired. It is in needed to overcome the Tekon Trigger limitation that trigger targets
     * must be Tekton resources.
     * 
     * The Agogos Trigger is backed by a Tekton Trigger. When that Tekton trigger fires, it creates
     * a CustomRun resource that will be handled by this controller. The controller itself does two
     * things during reconciliation:
     * 
     * 1. Creates the appropriate Agogos resource based on information provided by the CustomRun.
     * 2. Deletes the CustomRun resource.
     * 
     * The controller only processes CustomRun resources that are labeled appropriately. Other CustomRuns
     * are ignored.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BuildGeneratorController.class);

    private final static String TEKTON_TRIGGER_LABEL_PREFIX = "triggers.tekton.dev/";

    @Inject
    AgogosClient agogosClient;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Override
    public UpdateControl<CustomRun> reconcile(CustomRun resource, Context<CustomRun> context) {
        Boolean process = resource.getMetadata().getLabels().get(TriggerDependentResource.AGOGOS_CUSTOM_RUN_LABEL) != null &&
                Boolean.parseBoolean(resource.getMetadata().getLabels().get(TriggerDependentResource.AGOGOS_CUSTOM_RUN_LABEL));

        if (process) {
            // Include the Tekton trigger labels on the Build object.
            Map<String, String> triggerLabels = resource.getMetadata()
                    .getLabels()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().startsWith(TEKTON_TRIGGER_LABEL_PREFIX))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            String name = resource.getSpec().getCustomSpec().getSpec().get("name").toString();
            Resource kind = Resource.fromType(resource.getMetadata().getLabels().get(Resource.RESOURCE.getLabel()));
            switch (kind) {
                case COMPONENT:
                    Build build = new Build();
                    build.getMetadata().setGenerateName(name + "-");
                    build.getSpec().setComponent(name);
                    build.getMetadata().setLabels(triggerLabels);
                    build = agogosClient.v1alpha1().builds().inNamespace(resource.getMetadata().getNamespace()).resource(build)
                            .create();
                    LOG.debug("Trigger '{}' fired and created Build '{}'",
                            resource.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger"),
                            build.getMetadata().getName());
                    break;
                case PIPELINE:
                    Pipeline pipeline = new Pipeline();
                    pipeline.getMetadata().setGenerateName(name + "-");
                    pipeline.getMetadata().setLabels(triggerLabels);
                    pipeline = agogosClient.v1alpha1().pipelines().inNamespace(resource.getMetadata().getNamespace())
                            .resource(pipeline)
                            .create();
                    LOG.debug("Trigger '{}' fired and created Pipeline '{}'",
                            resource.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger"),
                            pipeline.getMetadata().getName());
                    break;
                default:
                    break;
            }

            kubernetesFacade.delete(resource);
        }
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(CustomRun resource, Context<CustomRun> context) {
        return DeleteControl.defaultDelete();
    }
}
