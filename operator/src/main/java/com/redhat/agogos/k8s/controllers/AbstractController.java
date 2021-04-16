package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.Status;
import com.redhat.agogos.eventing.CloudEventPublisher;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.TektonPipelineHelper;
import com.redhat.agogos.k8s.event.PipelineRunEvent;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.StatusResource;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractController<T extends AgogosResource<?, StatusResource>> implements ResourceController<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);

    @Inject
    TektonClient tektonClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CloudEventPublisher cloudEventPublisher;

    @Inject
    TektonPipelineHelper pipelineHelper;

    protected abstract AgogosResource<?, ?> parentResource(T resource);

    /**
     * <p>
     * Updates the status of particular Agogos resource based on the event received
     * from a Tekton {@link PipelineRun}.
     * </p>
     * 
     * <p>
     * This is the only place which contains logic for updating Agogos resource
     * statuses.
     * </p>
     * 
     * @param resource
     * @param event
     * @return {@link UpdateControl} object
     */
    public UpdateControl<T> handlePipelineRunEvent(T resource, PipelineRunEvent event) {
        String type = resource.getKind().toLowerCase();
        Status status = event.getStatus().toStatus();
        String message = null;
        Map<?, ?> result = null;

        switch (event.getStatus()) {
            case STARTED:
            case RUNNING:
                message = String.format("%s is running", resource.getKind());
                break;
            case COMPLETED:
                message = String.format("%s finished but some actions were skipped", resource.getKind());
                break;
            case SUCCEEDED:
                message = String.format("%s finished", resource.getKind());

                String resultJson = event.getResult();

                if (resultJson != null) {
                    try {
                        result = objectMapper.readValue(resultJson, Map.class);
                    } catch (JsonProcessingException e) {
                        // ceType = CloudEventType.BUILD_FAILURE; TODO ?!?!?!?
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
                    LOG.info("Tekton PipelineRun '{}' successfully cleaned up", pipelineRunName);
                } else {
                    LOG.warn("Could not remote '{}' Tekton PipelineRun", pipelineRunName);
                }

                break;
            case FAILED:
                message = String.format("%s failed", resource.getKind());
                break;
            case TIMEOUT:
                message = String.format("%s timed out", resource.getKind());
                break;
            case CANCELLING:
            case CANCELLED:
                message = "Build cancelled";
                break;
        }

        // Update status in the object and let us know whether it was necessary to
        // update it
        boolean update = setStatus(resource.getStatus(), status, message, result);

        // Update required, set new status and emit en event
        if (update) {
            try {
                cloudEventPublisher.publish(event.getStatus().toEvent(), resource, parentResource(resource));
            } catch (Exception e) {
                LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", type, resource.getKind(),
                        resource.getFullName(), e.getMessage(), e);
            }

            LOG.debug("Updating {} '{}' with Tekton PipelineRun state '{}'", resource.getKind(), resource.getFullName(),
                    event.getStatus());
            return UpdateControl.updateStatusSubResource(resource);
        }

        return UpdateControl.noUpdate();

    }

    /**
     * <p>
     * Triggers or updates the Tekton Pipeline based on the Custom Resource change
     * and sets the status subresource on it depending on the outcome of the
     * pipeline. update.
     * </p>
     * 
     * @param resource
     */
    @Override
    public UpdateControl<T> createOrUpdateResource(T resource, Context<T> context) {

        Optional<CustomResourceEvent> crEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);

        if (crEvent.isPresent()) {
            return handleResourceChange(resource);
        }

        Optional<PipelineRunEvent> pipelineRunEvent = context.getEvents().getLatestOfType(PipelineRunEvent.class);

        if (pipelineRunEvent.isPresent()) {
            return handlePipelineRunEvent(resource, pipelineRunEvent.get());
        }

        return UpdateControl.noUpdate();
    }

    public UpdateControl<T> handleResourceChange(T resource) {
        LOG.info("{} modified '{}'", resource.getKind(), resource.getFullName());

        try {
            switch (Status.valueOf(resource.getStatus().getStatus())) {
                case New:
                    return runNew(resource);
                case Running:
                    // TODO: Update the resource status when operator is restarted
                    // For any in-progress status - check the status of the dependent Tekton
                    // pipeline and update the build status
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            LOG.error("An error occurred while handling {} '{}' modification", resource.getKind(),
                    resource.getFullName(), ex);

            // Set status to "Failed" with reason being the failure
            setStatus(resource.getStatus(), Status.Failed, ex.getMessage());

            return UpdateControl.updateStatusSubResource(resource);
        }

        return UpdateControl.noUpdate();

    }

    /**
     * <p>
     * Updates {@link StatusResource} of the particular {@link CustomResource}.
     * <p/>
     * 
     * <p>
     * It sets result of the status to <code>null</code>.
     * </p>
     * 
     * @param status {@link StatusResource} object
     * @param newStatus One of available statuses
     * @param newReason Description of the reason for last status change
     */
    protected boolean setStatus(StatusResource status, Status newStatus, String newReason) {
        return setStatus(status, newStatus, newReason, null);
    }

    /**
     * <p>
     * Updates {@link StatusResource} of the particular {@link CustomResource}.
     * <p/>
     * 
     * @param status {@link StatusResource} object
     * @param newStatus One of available statuses
     * @param newReason Description of the reason for last status change
     * @param result Map of results, if any
     */
    protected boolean setStatus(StatusResource status, Status newStatus, String newReason,
            Map<?, ?> newResult) {

        if (status.getStatus().equals(String.valueOf(newStatus)) //
                && status.getReason().equals(newReason) //
                && Objects.equals(status.getResult(), newResult)) {

            return false;
        }

        status.setStatus(String.valueOf(newStatus));
        status.setReason(newReason);
        status.setResult(newResult);
        status.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }

    /**
     * <p>
     * Method responsible for creating a new Tekton {@link PipelineRun} responsible
     * to run a particular Agogos resource. This could include building a Component
     * or running a Pipeline.
     * </p>
     * 
     * @param resource
     * @return {@link UpdateControl} object
     */
    protected UpdateControl<T> runNew(T resource) {
        Map<String, String> labels = resource.getMetadata().getLabels();

        if (labels == null) {
            labels = new HashMap<>();
        }

        // In case the resource contains a 'pipelinerun' label this means that it
        // already has a Tekton PipelineRun associated with it, so skip creating new
        // one.
        if (labels.containsKey(Resource.PIPELINERUN.getLabel())) {
            LOG.warn(
                    "Tried to create a new Tekton PipelineRun for a resource with already bound PipelineRun, ignoring");
            return UpdateControl.noUpdate();
        }

        AgogosResource<?, ?> parent = parentResource(resource);

        if (!parent.isReady()) {
            LOG.warn("Parent resource {} '{}' of {} '{}' is not ready yet, failing", parent.getKind(),
                    parent.getFullName(), resource.getKind(), resource.getFullName());

            setStatus(resource.getStatus(), Status.Failed,
                    String.format("Parent resource '%s' is not ready", parent.getFullName()));

            return UpdateControl.updateStatusSubResource(resource);
        }

        LOG.info("Handling new {} '{}', triggering new Tekton PipelineRun for this resource", resource.getKind(),
                resource.getFullName());

        // First prepare the PipelineRun
        LOG.debug("Generating Tekton PipelineRun for {} resource '{}' and {} parent '{}'", resource.getKind(),
                resource.getFullName(), parent.getKind(), parent.getFullName());

        PipelineRun pipelineRun = pipelineHelper.generate(parent.getKind(), parent.getMetadata().getName(),
                parent.getMetadata().getNamespace(), resource);

        // Now we can deploy it in the cluster
        LOG.debug("Deploying Tekton PipelineRun");

        try {
            pipelineRun = pipelineHelper.run(pipelineRun, resource.getMetadata().getNamespace());
        } catch (Exception e) {
            String error = String.format("ERR-%s", UUID.randomUUID().toString());

            LOG.error("{}: Could not create Tekton PipelineRun", error, e);

            setStatus(resource.getStatus(), Status.Failed,
                    String.format("Could not prepare resource, ERROR: %s", error));

            return UpdateControl.updateStatusSubResource(resource);
        }

        // Add the 'pipelinerun' label to Agogos resource to point to new Tekton
        // PipelineRun
        labels.put(Resource.PIPELINERUN.getLabel(), pipelineRun.getMetadata().getName());

        // Update resource with new label
        resource.getMetadata().setLabels(labels);

        return UpdateControl.updateCustomResource(resource);
    }
}
