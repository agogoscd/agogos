package com.redhat.cpaas.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.eventing.CloudEventPublisher;
import com.redhat.cpaas.eventing.CloudEventType;
import com.redhat.cpaas.k8s.Resource;
import com.redhat.cpaas.k8s.TektonPipelineHelper;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.event.BuildEventSource;
import com.redhat.cpaas.k8s.event.PipelineRunEvent;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource.BuildStatus;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource.Status;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(generationAwareEventProcessing = false)
public class ComponentBuildController implements ResourceController<ComponentBuildResource> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentBuildController.class);

    @Inject
    ComponentResourceClient componentResourceClient;

    @Inject
    TektonPipelineHelper pipelineHelper;

    @Inject
    BuildEventSource pipelineRunEventSource;

    @Inject
    TektonClient tektonClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CloudEventPublisher cloudEventPublisher;

    /**
     * <p>
     * Register the {@link io.fabric8.tekton.pipeline.v1alpha1.PipelineRun} event
     * source so that we can receive events from PipelineRun's that are related to
     * {@link ComponentResource}'s.
     * </p>
     */
    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("pipeline-run", pipelineRunEventSource);
    }

    /**
     * <p>
     * Method triggered when a {@link ComponentBuildResource} is removed from the
     * cluster.
     * </p>
     * 
     * @param component {@link ComponentBuildResource}
     * @param context {@link Context}
     * @return {@link DeleteControl}
     */
    @Override
    public DeleteControl deleteResource(ComponentBuildResource build, Context<ComponentBuildResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    /**
     * <p>
     * Triggers or updates the Tekton Pipeline based on the
     * {@link ComponentBuildResource} data passed and sets the status subresource on
     * {@link ComponentBuildResource} depending on the outcome of the pipeline
     * update.
     * </p>
     * 
     * @param componentBuild {@link ComponentBuildResource}
     */
    @Override
    public UpdateControl<ComponentBuildResource> createOrUpdateResource(ComponentBuildResource componentBuild,
            Context<ComponentBuildResource> context) {

        Optional<CustomResourceEvent> crEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);

        if (crEvent.isPresent()) {
            LOG.debug("Build '{}' modified", componentBuild.getNamespacedName());

            try {
                switch (Status.valueOf(componentBuild.getStatus().getStatus())) {
                    case New:
                        LOG.info("Handling new ComponentBuild '{}'", componentBuild.getNamespacedName());

                        Map<String, String> labels = componentBuild.getMetadata().getLabels();

                        if (labels == null) {
                            labels = new HashMap<>();
                        }

                        if (!labels.containsKey(Resource.PIPELINE_RUN.getLabel())) {
                            // Run pipeline
                            PipelineRun pipelineRun = runBuildPipeline(componentBuild);

                            labels.put(Resource.PIPELINE_RUN.getLabel(), pipelineRun.getMetadata().getName());

                            componentBuild.getMetadata().setLabels(labels);

                            return UpdateControl.updateCustomResource(componentBuild);
                        }

                        // Set build status to "Running"
                        setStatus(componentBuild, Status.Running, "Build is running");
                        return UpdateControl.updateStatusSubResource(componentBuild);
                    case Running:
                        // TODO: Update the build status when operator is restarted
                        // For any in-progress status - check the status of the dependent Tekton
                        // pipeline and update the build status
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                LOG.error("An error occurred while handling build object '{}' modification",
                        componentBuild.getNamespacedName(), ex);

                // Set build status to "Failed"
                setStatus(componentBuild, Status.Failed, ex.getMessage());

                return UpdateControl.updateStatusSubResource(componentBuild);
            }
        }

        Optional<PipelineRunEvent> pipelineRunEvent = context.getEvents().getLatestOfType(PipelineRunEvent.class);

        if (pipelineRunEvent.isPresent()) {
            return handlePipelineRunEvent(componentBuild, pipelineRunEvent.get());
        }

        return UpdateControl.noUpdate();
    }

    /**
     * <p>
     * Updates {@link ComponentBuildResource.BuildStatus} of the particular
     * {@link ComponentBuildResource}.
     * <p/>
     * 
     * <p>
     * It sets result of the status to <code>null</code>.
     * </p>
     * 
     * 
     * @param component {@link ComponentBuildResource} object
     * @param status One of available statuses
     * @param reason Description of the reason for last status change
     */
    private boolean setStatus(ComponentBuildResource build, Status status, String reason) {
        return setStatus(build, status, reason, null);
    }

    /**
     * <p>
     * Updates {@link ComponentBuildResource.BuildStatus} of the particular
     * {@link ComponentBuildResource}.
     * <p/>
     * 
     * 
     * @param component {@link ComponentBuildResource} object
     * @param status One of available statuses
     * @param reason Description of the reason for last status change
     * @param result String formatted result, if any
     */
    private boolean setStatus(ComponentBuildResource build, Status status, String reason, Map<Object, Object> result) {
        BuildStatus buildStatus = build.getStatus();

        if (buildStatus.getStatus().equals(String.valueOf(status)) //
                && buildStatus.getReason().equals(reason) //
                && Objects.equals(buildStatus.getResult(), result)) {

            return false;
        }

        buildStatus.setStatus(String.valueOf(status));
        buildStatus.setReason(reason);
        buildStatus.setResult(result);
        buildStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }

    /**
     * <p>
     * Triggers Tekton pipeline of that is linked to the {@link ComponentResource}
     * the {@link ComponentBuildResource} is pointing to.
     * </p>
     * 
     * @param componentBuild The {@link ComponentBuildResource}
     * @throws ApplicationException in case the pipeline cannot be triggered
     */
    private PipelineRun runBuildPipeline(ComponentBuildResource componentBuild) throws ApplicationException {
        LOG.info("Triggering pipeline for build '{}'", componentBuild.getNamespacedName());

        ComponentResource component = componentResourceClient.getByName(componentBuild.getSpec().getComponent());

        if (component == null) {
            throw new MissingResourceException("Could not find component with name '{}' in namespace '{}'",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        if (!component.isReady()) {
            throw new ApplicationException("Component '{}'' is not ready to build it", component.getNamespacedName());
        }

        PipelineRun pipelineRun = pipelineHelper.generate(component.getKind(), component.getMetadata().getName(),
                component.getMetadata().getNamespace(), componentBuild);

        // TODO: refactr

        // We always set labels, so this is safe
        Map<String, String> labels = pipelineRun.getMetadata().getLabels();

        labels.put(Resource.COMPONENT.getLabel(), component.getMetadata().getName());

        pipelineRun.getMetadata().setLabels(labels);

        return pipelineHelper.run(pipelineRun, componentBuild.getMetadata().getNamespace());

    }

    private String cloudEventData(ComponentBuildResource build) {
        JsonObjectBuilder ceDataBuilder = Json.createObjectBuilder();

        ComponentResource component = componentResourceClient.getByName(build.getSpec().getComponent());

        if (component == null) {
            throw new ApplicationException("Could not find component with name '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        try {
            ceDataBuilder.add("build",
                    Json.createReader(new StringReader(objectMapper.writeValueAsString(build))).readValue());
            ceDataBuilder.add("component",
                    Json.createReader(new StringReader(objectMapper.writeValueAsString(component))).readValue());

        } catch (JsonProcessingException e) {
            throw new ApplicationException("Could not prepare CLoudEvent data", e);
        }

        return ceDataBuilder.build().toString();
    }

    /**
     * <p>
     * Updates the status of particular {@link ComponentBuildResource} based on the
     * event received from a {@link io.fabric8.tekton.pipeline.v1beta1.PipelineRun}.
     * </p>
     * 
     * @param build the build to
     * @param event
     * @return
     */
    public UpdateControl<ComponentBuildResource> handlePipelineRunEvent(ComponentBuildResource build,
            PipelineRunEvent event) {

        Status status = null;
        String message = null;
        Map<Object, Object> result = null;
        CloudEventType ceType = null;

        switch (event.getState()) {
            case STARTED:
            case RUNNING:
                ceType = CloudEventType.BUILD_START;
                status = Status.Running;
                message = "Build is running";
                break;
            case COMPLETED:
                ceType = CloudEventType.BUILD_SUCCESS;
                status = Status.Passed;
                message = "Build finished but some stages were skipped";
                break;
            case SUCCEEDED:
                ceType = CloudEventType.BUILD_SUCCESS;
                status = Status.Passed;
                message = "Build finished";

                String resultJson = event.getResult();

                if (resultJson != null) {
                    try {
                        result = objectMapper.readValue(resultJson, Map.class);
                    } catch (JsonProcessingException e) {
                        ceType = CloudEventType.BUILD_FAILURE;
                        status = Status.Failed;
                        message = "Build finished successfully, but returned metadata is not a valid JSON content";
                    }
                }

                String pipelineRunName = String.format("%s/%s", event.getPipelineRun().getMetadata().getNamespace(),
                        event.getPipelineRun().getMetadata().getName());

                LOG.info("Cleaning up Tekton PipelineRun '{}' after a successful build", pipelineRunName);

                boolean deleted = tektonClient.v1beta1().pipelineRuns()
                        .inNamespace(event.getPipelineRun().getMetadata().getNamespace())
                        .withName(event.getPipelineRun().getMetadata().getName()).delete();

                if (deleted) {
                    // TODO
                    LOG.info("Tekton PipelineRun '{}' successfully cleaned up", pipelineRunName);
                } else {
                    LOG.warn("Could not remote '{}' Tekton PipelineRun", pipelineRunName);
                }

                break;
            case FAILED:
                ceType = CloudEventType.BUILD_FAILURE;
                status = Status.Failed;
                message = "Build failed";
                break;
            case TIMEOUT:
                ceType = CloudEventType.BUILD_FAILURE;
                status = Status.Failed; // TODO: ??
                message = "Build timed out";
                break;
            case CANCELLING:
            case CANCELLED:
                ceType = CloudEventType.BUILD_FAILURE;
                status = Status.Failed; // TODO: ??
                message = "Build cancelled";
                break;
        }

        // Update status in the object and let us know whether it was necessary to
        // update it
        boolean update = setStatus(build, status, message, result);

        // Update required, set new status and emit en event
        if (update) {
            try {
                cloudEventPublisher.publish(ceType, cloudEventData(build));
            } catch (Exception e) {
                LOG.warn("Could not publish CloudEvent for build '{}'", build.getNamespacedName(), e);
            }

            LOG.debug("Updating build '{}' with PipelineRun state '{}'", build.getNamespacedName(), event.getState());
            return UpdateControl.updateStatusSubResource(build);
        }

        return UpdateControl.noUpdate();

    }
}
