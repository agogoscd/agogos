package com.redhat.agogos.operator.k8s.controllers.run;

import com.redhat.agogos.core.PipelineRunStatus;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.errors.ApplicationException;
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
    public UpdateControl<Run> reconcile(Run resource, Context<Run> context) {

        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (optional.isEmpty()) {
            LOG.debug("No PipelineRun for '{}' yet, returning noUpdate", resource.getFullName());
            return UpdateControl.noUpdate();
        } else if (optional.get().getStatus() == null) {
            LOG.debug("No PipelineRun status for '{}' yet, returning noUpdate", resource.getFullName());
            return UpdateControl.noUpdate();
        }

        PipelineRun pipelinerun = optional.get();
        PipelineRunStatus runStatus = PipelineRunStatus.fromPipelineRun(pipelinerun);
        ResultableResourceStatus status = runStatus.toStatus();

        LOG.debug("PipelineRun status for '{}' is {}", resource.getFullName(), runStatus);

        ResultableStatus resourceStatus = resource.getStatus();
        final ResultableStatus originalResourceStatus = objectMapper.clone(resourceStatus);

        String message = null;
        Map<Object, Object> result = null;

        switch (runStatus) {
            case STARTED:
                message = String.format("%s started", resource.getKind());
                break;
            case RUNNING:
                message = String.format("%s is running", resource.getKind());
                break;
            case COMPLETED:
                message = String.format("%s finished but some actions were skipped", resource.getKind());
                break;
            case SUCCEEDED:
                message = String.format("%s finished", resource.getKind());

                List<PipelineRunResult> results = pipelinerun.getStatus().getPipelineResults().stream()
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
                message = String.format("%s failed", resource.getKind());
                break;
            case TIMEOUT:
                message = String.format("%s timed out", resource.getKind());
                break;
            case CANCELLING:
                message = String.format("%s is being cancelled", resource.getKind());
                break;
            case CANCELLED:
                message = String.format("%s cancelled", resource.getKind());
                break;
        }

        resourceStatus.setStatus(status);
        resourceStatus.setReason(message);
        resourceStatus.setResult(result);
        resourceStatus.setStartTime(pipelinerun.getStatus().getStartTime());
        resourceStatus.setCompletionTime(pipelinerun.getStatus().getCompletionTime());

        // Check whether the resource status was modified
        // If this is not the case, we are done here
        if (resourceStatus.equals(originalResourceStatus)) {
            LOG.debug("No change to {} status of '{}', returning noUpdate", resource.getKind(), resource.getFullName());
            return UpdateControl.noUpdate();
        }

        // Update the last update field
        resourceStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        try {
            cloudEventPublisher.publish(runStatus.toEvent(), resource, parentResource(resource, context));
        } catch (Exception e) {
            LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", resource.getKind().toLowerCase(),
                    resource.getKind(), resource.getFullName(), e.getMessage(), e);
        }

        LOG.debug("Updating {} '{}' with status '{}' (PipelineRun state '{}')",
                resource.getKind(), resource.getFullName(), status, runStatus);

        return UpdateControl.updateStatus(resource);
    }

    @Override
    protected Pipeline parentResource(Run run, Context<Run> context) {
        Pipeline pipeline = kubernetesFacade.get(
                Pipeline.class,
                run.getMetadata().getNamespace(),
                run.getSpec().getPipeline());
        if (pipeline == null) {
            throw new ApplicationException("Could not find Pipeline '{}' in namespace '{}'",
                    run.getSpec().getPipeline(), run.getMetadata().getNamespace());
        }

        return pipeline;
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
