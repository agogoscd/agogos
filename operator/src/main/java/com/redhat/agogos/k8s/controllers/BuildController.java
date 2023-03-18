package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.k8s.controllers.dependent.BuildPipelineRunDependentResource;
import com.redhat.agogos.v1alpha1.Build;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = BuildPipelineRunDependentResource.class) })
public class BuildController extends AbstractRunController<Build> {

    // private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);

    // @Override
    // public UpdateControl<Build> reconcile(Build build, Context<Build> context) {
    //     Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
    //     if (!optional.isPresent()) {
    //         LOG.debug("No pipeline run for build {} yet, ignoring", build.getFullName());
    //         return UpdateControl.noUpdate();
    //     }

    //     PipelineRun pipelinerun = optional.get();
    //     PipelineRunStatus runStatus = PipelineRunStatus.fromPipelineRun(pipelinerun);

    //     String type = build.getKind().toLowerCase();
    //     ResultableResourceStatus status = runStatus.toStatus();

    //     ResultableStatus resourceStatus = build.getStatus();
    //     final ResultableStatus originalResourceStatus = deepCopy(resourceStatus);

    //     String message = null;
    //     Map<?, ?> result = null;

    //     switch (runStatus) {
    //         case STARTED:
    //             message = String.format("%s started", build.getKind());
    //             break;
    //         case RUNNING:
    //             message = String.format("%s is running", build.getKind());
    //             break;
    //         case COMPLETED:
    //             message = String.format("%s finished but some actions were skipped", build.getKind());
    //             break;
    //         case SUCCEEDED:
    //             message = String.format("%s finished", build.getKind());

    //             String resultJson = null;
    //             List<PipelineRunResult> results = pipelinerun.getStatus().getPipelineResults().stream()
    //                     .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

    //             if (!results.isEmpty()) {
    //                 resultJson = results.get(0).getValue().getStringVal();
    //             }

    //             if (resultJson != null) {
    //                 try {
    //                     result = objectMapper.readValue(resultJson, Map.class);
    //                 } catch (JsonProcessingException e) {
    //                     // ceType = CloudEventType.BUILD_FAILURE; TODO ?!?!?!?
    //                     status = ResultableResourceStatus.Failed;
    //                     message = "Build finished successfully, but returned metadata is not a valid JSON content";
    //                 }
    //             }

    //             String pipelineRunName = String.format("%s/%s", pipelinerun.getMetadata().getNamespace(),
    //                     pipelinerun.getMetadata().getName());

    //             LOG.info("Cleaning up Tekton PipelineRun '{}' after a successful build", pipelineRunName);

    //             // This may or may not remove the Tekton PipelineRun
    //             // In case it is not successful (for any reason) it will be hanging there.
    //             // It is the ops duty to find out why these were not removed and clean them up.
    //             tektonClient.v1beta1().pipelineRuns()
    //                     .inNamespace(pipelinerun.getMetadata().getNamespace())
    //                     .withName(pipelinerun.getMetadata().getName()).delete();

    //             break;
    //         case FAILED:
    //             message = String.format("%s failed", build.getKind());
    //             break;
    //         case TIMEOUT:
    //             message = String.format("%s timed out", build.getKind());
    //             break;
    //         case CANCELLING:
    //             message = String.format("%s is being cancelled", build.getKind());
    //             break;
    //         case CANCELLED:
    //             message = String.format("%s cancelled", build.getKind());
    //             break;
    //     }

    //     resourceStatus.setStatus(String.valueOf(status));
    //     resourceStatus.setReason(message);
    //     resourceStatus.setResult(result);
    //     resourceStatus.setStartTime(pipelinerun.getStatus().getStartTime());
    //     resourceStatus.setCompletionTime(pipelinerun.getStatus().getCompletionTime());

    //     // Check whether the resource status was modified
    //     // If this is not the case, we are done here
    //     if (resourceStatus.equals(originalResourceStatus)) {
    //         LOG.debug("No change to status of build {}, ignoring", build.getFullName());
    //         return UpdateControl.noUpdate();
    //     }

    //     try {
    //         cloudEventPublisher.publish(runStatus.toEvent(), build, build.parentResource());
    //     } catch (Exception e) {
    //         LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", type, build.getKind(),
    //                 build.getFullName(), e.getMessage(), e);
    //     }

    //     // Update the last update field
    //     resourceStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

    //     LOG.debug("Updating {} '{}' with Tekton PipelineRun state '{}'", build.getKind(), build.getFullName(),
    //             runStatus);

    //     return UpdateControl.updateStatus(build);
    // }
}
