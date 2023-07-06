package com.redhat.agogos.k8s.controllers.build;

import com.redhat.agogos.PipelineRunStatus;
import com.redhat.agogos.ResultableResourceStatus;
import com.redhat.agogos.k8s.controllers.AbstractController;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ResultableBuildStatus;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunResult;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = PipelineRunDependentResource.class, reconcilePrecondition = PipelineRunPrecondition.class)
})
public class BuildController extends AbstractController<Build> implements EventSourceInitializer<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);
    public static final String COMPONENT_INDEX = "ComponentIndex";

    @SuppressWarnings("unchecked")
    @Override
    public UpdateControl<Build> reconcile(Build build, Context<Build> context) {

        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (optional.isEmpty()) {
            LOG.debug("No PipelineRun for '{}' yet, returning noUpdate", build.getFullName());
            return UpdateControl.noUpdate();
        } else if (optional.get().getStatus() == null) {
            LOG.debug("No PipelineRun status for '{}' yet, returning noUpdate", build.getFullName());
            return UpdateControl.noUpdate();
        }

        PipelineRun pipelinerun = optional.get();
        PipelineRunStatus runStatus = PipelineRunStatus.fromPipelineRun(pipelinerun);
        ResultableResourceStatus status = runStatus.toStatus();

        LOG.debug("PipelineRun status for '{}' is {}", build.getFullName(), runStatus);

        ResultableBuildStatus resourceStatus = build.getStatus();
        final ResultableBuildStatus originalResourceStatus = objectMapper.clone(resourceStatus);

        String message = null;
        Map<Object, Object> result = null;

        switch (runStatus) {
            case STARTED:
                message = String.format("%s started", build.getKind());
                break;
            case RUNNING:
                message = String.format("%s is running", build.getKind());
                break;
            case COMPLETED:
                message = String.format("%s finished but some actions were skipped", build.getKind());
                break;
            case SUCCEEDED:
                message = String.format("%s finished", build.getKind());

                List<PipelineRunResult> results = pipelinerun.getStatus().getPipelineResults().stream()
                        .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

                if (!results.isEmpty()) {
                    String resultJson = results.get(0).getValue().getStringVal();
                    result = objectMapper.convertValue(resultJson, Map.class);
                }
                break;
            case FAILED:
                message = String.format("%s failed", build.getKind());
                break;
            case TIMEOUT:
                message = String.format("%s timed out", build.getKind());
                break;
            case CANCELLING:
                message = String.format("%s is being cancelled", build.getKind());
                break;
            case CANCELLED:
                message = String.format("%s cancelled", build.getKind());
                break;
        }

        Component component = parentResource(build, context);
        resourceStatus.setComponentSpec(component.getSpec());
        resourceStatus.setStatus(status);
        resourceStatus.setReason(message);
        resourceStatus.setResult(result);
        resourceStatus.setStartTime(pipelinerun.getStatus().getStartTime());
        resourceStatus.setCompletionTime(pipelinerun.getStatus().getCompletionTime());

        // Check whether the resource status was modified
        // If this is not the case, we are done here
        if (resourceStatus.equals(originalResourceStatus)) {
            LOG.debug("No change to {} status of '{}', returning noUpdate", build.getKind(), build.getFullName());
            return UpdateControl.noUpdate();
        }

        // Update the last update field
        resourceStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        try {
            cloudEventPublisher.publish(runStatus.toEvent(), build, component);
        } catch (Exception e) {
            LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", build.getKind().toLowerCase(),
                    build.getKind(), build.getFullName(), e.getMessage(), e);
        }

        LOG.debug("Updating {} '{}' with status '{}' (PipelineRun state '{}')",
                build.getKind(), build.getFullName(), status, runStatus);

        return UpdateControl.updateStatus(build);
    }

    @Override
    protected Component parentResource(Build build, Context<Build> context) {
        return context.getSecondaryResource(Component.class).orElseThrow();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Build> context) {
        context.getPrimaryCache().addIndexer(COMPONENT_INDEX,
                b -> List.of(indexKey(b.getSpec().getComponent(), b.getMetadata().getNamespace())));
        InformerEventSource<Component, Build> componentES = new InformerEventSource<>(InformerConfiguration
                .from(Component.class, context)
                .withSecondaryToPrimaryMapper(component -> context.getPrimaryCache().byIndex(COMPONENT_INDEX,
                        indexKey(component.getMetadata().getName(), component.getMetadata().getNamespace())).stream()
                        .map(ResourceID::fromResource).collect(Collectors.toSet()))
                .withPrimaryToSecondaryMapper((PrimaryToSecondaryMapper<Build>) primary -> Set.of(
                        new ResourceID(primary.getSpec().getComponent(), primary.getMetadata().getNamespace())))
                .build(), context);
        return EventSourceInitializer.nameEventSources(componentES);
    }

    private String indexKey(String name, String namespace) {
        return name + "#" + namespace;
    }
}
