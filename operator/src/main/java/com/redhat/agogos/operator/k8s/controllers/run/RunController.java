package com.redhat.agogos.operator.k8s.controllers.run;

import com.redhat.agogos.core.PipelineRunStatus;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.ResultableStatus;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
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
public class RunController extends AbstractController<Run> implements EventSourceInitializer<Run> {

    private static final Logger LOG = LoggerFactory.getLogger(RunController.class);
    public static final String PIPELINE_INDEX = "PipelineIndex";

    @SuppressWarnings("unchecked")
    @Override
    public UpdateControl<Run> reconcile(Run run, Context<Run> context) {

        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (optional.isEmpty()) {
            LOG.debug("No PipelineRun for '{}' yet, returning noUpdate", run.getFullName());
            return UpdateControl.noUpdate();
        } else if (optional.get().getStatus() == null) {
            LOG.debug("No PipelineRun status for '{}' yet, returning noUpdate", run.getFullName());
            return UpdateControl.noUpdate();
        }

        PipelineRun pipelineRun = optional.get();
        PipelineRunStatus runStatus = PipelineRunStatus.fromPipelineRun(pipelineRun);
        ResultableResourceStatus status = runStatus.toStatus();

        LOG.debug("PipelineRun status for '{}' is {}", run.getFullName(), runStatus);

        ResultableStatus resourceStatus = run.getStatus();
        final ResultableStatus originalResourceStatus = objectMapper.clone(resourceStatus);

        String message = null;
        Map<Object, Object> result = null;

        switch (runStatus) {
            case STARTED:
                message = String.format("%s started", run.getKind());
                break;
            case RESOLVINGTASKREF:
                message = String.format("%s is resolving a task reference", run.getKind());
                break;
            case RUNNING:
                message = String.format("%s is running", run.getKind());
                break;
            case COMPLETED:
                message = String.format("%s finished but some actions were skipped", run.getKind());
                break;
            case SUCCEEDED:
                message = String.format("%s finished", run.getKind());

                List<PipelineRunResult> results = pipelineRun.getStatus().getPipelineResults().stream()
                        .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

                if (!results.isEmpty()) {
                    String resultJson = results.get(0).getValue().getStringVal();
                    try {
                        result = objectMapper.convertValue(resultJson, Map.class);
                    } catch (Exception e) {
                        status = ResultableResourceStatus.FAILED;
                        message = "Build finished successfully, but returned metadata is not a valid JSON content";
                    }
                }
                break;
            case FAILED:
                message = String.format("%s failed", run.getKind());
                break;
            case TIMEOUT:
                message = String.format("%s timed out", run.getKind());
                break;
            case CANCELLING:
                message = String.format("%s is being cancelled", run.getKind());
                break;
            case CANCELLED:
                message = String.format("%s cancelled", run.getKind());
                break;
        }

        Pipeline pipeline = parentResource(run, context);
        resourceStatus.setStatus(status);
        resourceStatus.setReason(message);
        resourceStatus.setResult(result);
        if (resourceStatus.getStartTime() == null) {
            resourceStatus.setStartTime(ResultableStatus.getFormattedNow());
        }
        if (pipelineRun.getStatus().getCompletionTime() != null && resourceStatus.getCompletionTime() == null) {
            resourceStatus.setCompletionTime(ResultableStatus.getFormattedNow());
        }

        // Check whether the resource status was modified
        // If this is not the case, we are done here
        if (resourceStatus.equals(originalResourceStatus)) {
            LOG.debug("No change to {} status of '{}', returning noUpdate", run.getKind(), run.getFullName());
            return UpdateControl.noUpdate();
        }

        // Update the last update field
        resourceStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        // Update now, BEFORE sending the cloud event. Otherwise we are in a race condition for any interceptors.
        UpdateControl<Run> ctrl = UpdateControl.updateResourceAndStatus(run);

        if (originalResourceStatus.getStatus() != resourceStatus.getStatus()) {
            try {
                cloudEventPublisher.publish(runStatus.toEvent(), run, pipeline);
            } catch (Exception e) {
                LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", run.getKind().toLowerCase(),
                        run.getKind(), run.getFullName(), e.getMessage(), e);
            }
        } else {
            LOG.debug("Not sending CloudEvent, original status '{}', new status '{}'", originalResourceStatus.getStatus(),
                    resourceStatus.getStatus());
        }

        updateExecutionInformation(run, pipeline.getMetadata().getName(), resourceStatus);

        LOG.debug("Updating {} '{}' with status '{}' (PipelineRun state '{}')",
                run.getKind(), run.getFullName(), status, runStatus);

        return ctrl;
    }

    @Override
    protected Pipeline parentResource(Run run, Context<Run> context) {
        return context.getSecondaryResource(Pipeline.class).orElseThrow();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Run> context) {
        context.getPrimaryCache().addIndexer(PIPELINE_INDEX,
                b -> List.of(indexKey(b.getSpec().getPipeline(), b.getMetadata().getNamespace())));
        InformerEventSource<Pipeline, Run> pipelineES = new InformerEventSource<>(InformerConfiguration
                .from(Pipeline.class, context)
                .withSecondaryToPrimaryMapper(pipeline -> context.getPrimaryCache().byIndex(PIPELINE_INDEX,
                        indexKey(pipeline.getMetadata().getName(), pipeline.getMetadata().getNamespace())).stream()
                        .map(ResourceID::fromResource).collect(Collectors.toSet()))
                .withPrimaryToSecondaryMapper((PrimaryToSecondaryMapper<Run>) primary -> Set.of(
                        new ResourceID(primary.getSpec().getPipeline(), primary.getMetadata().getNamespace())))
                .build(), context);
        return EventSourceInitializer.nameEventSources(pipelineES);
    }

    private String indexKey(String name, String namespace) {
        return name + "#" + namespace;
    }
}
