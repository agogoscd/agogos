package com.redhat.cpaas.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.eventing.CloudEventPublisher;
import com.redhat.cpaas.eventing.CloudEventType;
import com.redhat.cpaas.k8s.TektonPipelineHelper;
import com.redhat.cpaas.k8s.client.PipelineClient;
import com.redhat.cpaas.k8s.event.PipelineEventSource;
import com.redhat.cpaas.k8s.event.PipelineRunEvent;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import com.redhat.cpaas.v1alpha1.PipelineResource;
import com.redhat.cpaas.v1alpha1.PipelineRunResource;
import com.redhat.cpaas.v1alpha1.PipelineRunResource.RunStatus;
import com.redhat.cpaas.v1alpha1.PipelineRunResource.Status;
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
import java.util.Optional;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(generationAwareEventProcessing = false)
public class PipelineRunController implements ResourceController<PipelineRunResource> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunController.class);

    @Inject
    TektonPipelineHelper pipelineHelper;

    @Inject
    PipelineClient pipelineClient;

    @Inject
    CloudEventPublisher cloudEventPublisher;

    @Inject
    PipelineEventSource pipelineRunEventSource;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public DeleteControl deleteResource(PipelineRunResource run, Context<PipelineRunResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

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

    @Override
    public UpdateControl<PipelineRunResource> createOrUpdateResource(PipelineRunResource pipelineRun,
            Context<PipelineRunResource> context) {

        Optional<CustomResourceEvent> crEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);

        if (crEvent.isPresent()) {

            LOG.info("PipelineRun '{}' modified", pipelineRun.getNamespacedName());

            try {
                switch (Status.valueOf(pipelineRun.getStatus().getStatus())) {
                    case New:
                        LOG.info("Handling new Pipeline run '{}'", pipelineRun.getNamespacedName());

                        PipelineResource pipeline = null;

                        try {
                            pipeline = pipelineClient.getByName(pipelineRun.getSpec().getPipeline(),
                                    pipelineRun.getMetadata().getNamespace());
                        } catch (MissingResourceException e) {
                            LOG.error("Could not find Pipeline '{}' in '{}' namespace", pipelineRun.getSpec().getPipeline(),
                                    pipelineRun.getMetadata().getNamespace(), e);

                            setStatus(pipelineRun, Status.Failed, e.getMessage());
                            return UpdateControl.updateStatusSubResource(pipelineRun);
                        }

                        JsonObjectBuilder ceDataBuilder = Json.createObjectBuilder();

                        try {
                            ceDataBuilder.add("run",
                                    Json.createReader(new StringReader(objectMapper.writeValueAsString(pipelineRun)))
                                            .readValue());
                            ceDataBuilder.add("pipeline", Json
                                    .createReader(new StringReader(objectMapper.writeValueAsString(pipeline))).readValue());
                        } catch (JsonProcessingException e) {
                            LOG.error("Error while preparing CloudEvent resource", e);

                            setStatus(pipelineRun, Status.Failed,
                                    "Internal error occurred while generating PipelineRun metadata");

                            return UpdateControl.updateStatusSubResource(pipelineRun);
                        }

                        // Run Tekton pipeline
                        try {
                            PipelineRun tektonPipelineRun = pipelineHelper.run(pipeline.getKind(),
                                    pipelineRun.getSpec().getPipeline(), pipelineRun.getMetadata().getNamespace(),
                                    pipelineRun);

                            LOG.info("Tekton Pipeline '{}' was run as '{}' in '{}' namespace",
                                    pipeline.getMetadata().getName(), tektonPipelineRun.getMetadata().getName(),
                                    tektonPipelineRun.getMetadata().getNamespace());

                        } catch (MissingResourceException e) {
                            LOG.error("Error while running Tekton Pipeline '{}'", pipelineRun.getSpec().getPipeline(), e);

                            setStatus(pipelineRun, Status.Failed, e.getMessage());
                            return UpdateControl.updateStatusSubResource(pipelineRun);
                        }

                        setStatus(pipelineRun, Status.Running, "Pipeline is running");
                        cloudEventPublisher.publish(CloudEventType.PIPELINE_START, ceDataBuilder.build().toString());
                        return UpdateControl.updateStatusSubResource(pipelineRun);
                    default:
                        break;
                }
            } catch (Exception ex) {
                LOG.error("An error occurred while handling PipelineRun object '{}' modification",
                        pipelineRun.getNamespacedName(), ex);

                // Set build status to "Failed"
                setStatus(pipelineRun, Status.Failed, ex.getMessage());

                return UpdateControl.updateStatusSubResource(pipelineRun);
            }
        }

        Optional<PipelineRunEvent> pipelineRunEvent = context.getEvents().getLatestOfType(PipelineRunEvent.class);

        if (pipelineRunEvent.isPresent()) {
            // return handlePipelineRunEvent(componentBuild, pipelineRunEvent.get());
        }

        return UpdateControl.noUpdate();
    }

    // TODO: Make this shared across controllers
    private boolean setStatus(PipelineRunResource run, Status status, String reason) {
        RunStatus runStatus = run.getStatus();

        if (runStatus.getStatus().equals(String.valueOf(status)) && runStatus.getReason().equals(reason)) {
            return false;
        }

        runStatus.setStatus(String.valueOf(status));
        runStatus.setReason(reason);
        runStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }
}
