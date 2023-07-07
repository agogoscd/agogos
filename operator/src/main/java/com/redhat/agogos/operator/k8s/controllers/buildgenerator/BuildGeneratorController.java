package com.redhat.agogos.operator.k8s.controllers.buildgenerator;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Run;
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
     * 1. Creates the appropriate Agogos resources based on information provided by the CustomRun.
     * 2. Deletes the CustomRun resource.
     * 
     * The controller only processes CustomRun resources that are labeled appropriately. Other CustomRuns
     * are ignored.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BuildGeneratorController.class);

    private final static String TEKTON_TRIGGER_LABEL_PREFIX = "triggers.tekton.dev/";

    @Inject
    KubernetesFacade kubernetesFacade;

    @Override
    public UpdateControl<CustomRun> reconcile(CustomRun resource, Context<CustomRun> context) {
        Boolean process = resource.getMetadata().getLabels().get(TriggerDependentResource.AGOGOS_CUSTOM_RUN_LABEL) != null &&
                Boolean.parseBoolean(resource.getMetadata().getLabels().get(TriggerDependentResource.AGOGOS_CUSTOM_RUN_LABEL));

        if (process) {
            // Include the Tekton trigger labels on the Build object.
            Map<String, String> labels = resource.getMetadata()
                    .getLabels()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().startsWith(TEKTON_TRIGGER_LABEL_PREFIX))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            String name = resource.getSpec().getCustomSpec().getSpec().get("name").toString();
            Resource kind = Resource.fromType(resource.getMetadata().getLabels().get(Resource.RESOURCE.getResourceLabel()));
            switch (kind) {
                case COMPONENT:
                    submitBuild(resource, name, labels);
                    break;
                case GROUP:
                    Group group = kubernetesFacade.get(Group.class, resource.getMetadata().getNamespace(), name);
                    group.getSpec().getComponents().stream().forEach(component -> {
                        submitBuild(resource, component, labels);
                    });
                    group.getSpec().getPipelines().stream().forEach(pipeline -> {
                        submitPipeline(resource, pipeline, labels);
                    });
                    break;
                case PIPELINE:
                    submitPipeline(resource, name, labels);
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

    private void submitBuild(CustomRun resource, String name, Map<String, String> labels) {
        Build build = new Build();
        build.getMetadata().setGenerateName(name + "-");
        build.getMetadata().setNamespace(resource.getMetadata().getNamespace());
        build.getSpec().setComponent(name);
        labels.put(Resource.getInstanceLabel(),
                resource.getMetadata().getLabels().get(Resource.getInstanceLabel()));
        build.getMetadata().setLabels(labels);
        build = kubernetesFacade.create(build);
        LOG.debug("Trigger '{}' fired and created Build '{}'",
                resource.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger"),
                build.getMetadata().getName());
    }

    private void submitPipeline(CustomRun resource, String name, Map<String, String> labels) {
        Run run = new Run();
        run.getMetadata().setGenerateName(name + "-");
        run.getMetadata().setNamespace(resource.getMetadata().getNamespace());
        run.getSpec().setPipeline(name);
        labels.put(Resource.getInstanceLabel(),
                resource.getMetadata().getLabels().get(Resource.getInstanceLabel()));
        run.getMetadata().setLabels(labels);
        run = kubernetesFacade.create(run);
        LOG.debug("Trigger '{}' fired and created Run '{}'",
                resource.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger"),
                run.getMetadata().getName());
    }
}
