package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.agogos.PipelineRunStatus;
import com.redhat.agogos.ResultableResourceStatus;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.ResultableStatus;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunResult;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractRunController<T extends AgogosResource<?, ResultableStatus>> extends AbstractController<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);

    @Override
    public UpdateControl<T> reconcile(T resource, Context<T> context) {
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
        final ResultableStatus originalResourceStatus = deepCopy(resourceStatus);

        String message = null;
        Map<?, ?> result = null;

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

                String resultJson = null;
                List<PipelineRunResult> results = pipelinerun.getStatus().getPipelineResults().stream()
                        .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

                if (!results.isEmpty()) {
                    resultJson = results.get(0).getValue().getStringVal();
                }

                if (resultJson != null) {
                    try {
                        result = objectMapper.readValue(resultJson, Map.class);
                    } catch (JsonProcessingException e) {
                        // ceType = CloudEventType.BUILD_FAILURE; TODO ?!?!?!?
                        status = ResultableResourceStatus.Failed;
                        message = "Build finished successfully, but returned metadata is not a valid JSON content";
                    }
                }

                // String pipelineRunName = String.format("%s/%s", pipelinerun.getMetadata().getNamespace(),
                //         pipelinerun.getMetadata().getName());

                // LOG.info("Cleaning up PipelineRun '{}' after a successful {}", pipelineRunName,
                //         resource.getKind().toLowerCase());

                // // This may or may not remove the Tekton PipelineRun
                // // In case it is not successful (for any reason) it will be hanging there.
                // // It is the ops duty to find out why these were not removed and clean them up.
                // tektonClient.v1beta1().pipelineRuns()
                //         .inNamespace(pipelinerun.getMetadata().getNamespace())
                //         .withName(pipelinerun.getMetadata().getName()).delete();

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

        resourceStatus.setStatus(String.valueOf(status));
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
            cloudEventPublisher.publish(runStatus.toEvent(), resource, parentResource(resource));
        } catch (Exception e) {
            LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", resource.getKind().toLowerCase(),
                    resource.getKind(), resource.getFullName(), e.getMessage(), e);
        }

        LOG.debug("Updating {} '{}' with PipelineRun state '{}'", resource.getKind(), resource.getFullName(),
                runStatus);

        return UpdateControl.updateStatus(resource);
    }
}