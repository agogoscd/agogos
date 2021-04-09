package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.BuilderClient;
import com.redhat.agogos.k8s.client.PipelineClient;
import com.redhat.agogos.v1alpha1.BuilderResource;
import com.redhat.agogos.v1alpha1.ComponentResource;
import com.redhat.agogos.v1alpha1.ComponentResource.ComponentStatus;
import com.redhat.agogos.v1alpha1.ComponentResource.Status;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineResult;
import io.fabric8.tekton.pipeline.v1beta1.PipelineResultBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclarationBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRef;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ComponentController implements ResourceController<ComponentResource> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentController.class);

    @Inject
    PipelineClient componentResourceClient;

    @Inject
    BuilderClient builderClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TektonClient tektonClient;

    /**
     * <p>
     * Method triggered when a {@link ComponentResource} is removed from the
     * cluster.
     * </p>
     * 
     * @param component {@link ComponentResource}
     * @param context {@link Context}
     * @return {@link DeleteControl}
     */
    @Override
    public DeleteControl deleteResource(ComponentResource component, Context<ComponentResource> context) {
        LOG.info("Removing component '{}'", component.getNamespacedName());
        return DeleteControl.DEFAULT_DELETE;
    }

    /**
     * <p>
     * Main method that is triggered when a change on the {@link ComponentResource}
     * object is detected.
     * </p>
     * 
     * @param component {@link ComponentResource}
     * @param context {@link Context}
     * @return {@link UpdateControl}
     */
    @Override
    public UpdateControl<ComponentResource> createOrUpdateResource(ComponentResource component,
            Context<ComponentResource> context) {

        // Try to find the latest event
        final Optional<CustomResourceEvent> customResourceEvent = context.getEvents()
                .getLatestOfType(CustomResourceEvent.class);

        // Handle it if available
        if (customResourceEvent.isPresent()) {
            return onResourceUpdate(component, context);
        }

        return UpdateControl.noUpdate();
    }

    /**
     * <p>
     * Updates {@link ComponentResource.ComponentStatus} of the particular
     * {@link ComponentResource}.
     * <p/>
     * 
     * 
     * @param component {@link ComponentResource} object
     * @param status One of available statuses
     * @param reason Description of the reason for last status change
     */
    private void setStatus(final ComponentResource component, final Status status, final String reason) {
        ComponentStatus componentStatus = component.getStatus();

        componentStatus.setStatus(String.valueOf(status));
        componentStatus.setReason(reason);
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    }

    /**
     * <p>
     * Creates or updates Tekton pipeline based on the {@link ComponentResource}
     * data passed and sets the status subresource on {@link ComponentResource}
     * depending on the outcome of the pipeline update.
     * </p>
     * 
     * @param component Component resource to create the pipeline for
     */
    private void updateBuildPipeline(ComponentResource component) {
        try {
            LOG.debug("Preparing pipeline for component '{}'", component.getNamespacedName());

            this.updateTektonBuildPipeline(component);

            setStatus(component, Status.Initializing, "Preparing pipeline");

            LOG.info("Pipeline for component '{}' updated", component.getNamespacedName());
        } catch (ApplicationException e) {
            LOG.error("Error occurred while creating pipeline for component '{}'", component.getNamespacedName(), e);

            setStatus(component, Status.Failed, "Could not create component pipeline");
        }
    }

    private UpdateControl<ComponentResource> onResourceUpdate(ComponentResource component,
            Context<ComponentResource> context) {
        LOG.info("Component '{}' modified", component.getNamespacedName());

        // Create or update pipeline in any case
        this.updateBuildPipeline(component);

        // Update Component status
        setStatus(component, Status.Ready, "");

        return UpdateControl.updateStatusSubResource(component);
    }

    /**
     * <p>
     * Creates or updates Tekton pipeline based on the {@link ComponentResource}
     * data passed.
     * </p>
     * 
     * @param component {@link ComponentResource} to create the pipeline for
     * @return {@link Pipeline} object for the updated pipeline
     * @throws ApplicationException in case the pipeline cannot be updated
     */
    private Pipeline updateTektonBuildPipeline(ComponentResource component) throws ApplicationException {
        String componentJson;

        // Convert Component metadata to JSON
        // TODO: Remove this
        try {
            componentJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(component.toEasyMap());
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Internal error; could not serialize component '{}'",
                    component.getNamespacedName(), e);
        }

        BuilderResource builder = builderClient.getByName(component.getSpec().getBuilder());

        if (builder == null) {
            throw new MissingResourceException("Selected Builder '{}' is not available in the system",
                    component.getSpec().getBuilder());
        }

        List<PipelineTask> tasks = new ArrayList<>();

        // Prepare workspace for main task to store results
        WorkspacePipelineTaskBinding stageWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("stage") //
                .withWorkspace("ws") //
                .withSubPath("stage") //
                .build();

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding pipelineWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("pipeline") //
                .withWorkspace("ws") //
                .withSubPath("pipeline") //
                .build();

        PipelineTask initTask = new PipelineTaskBuilder() //
                .withName("init") //
                .withTaskRef(new TaskRef("tekton.dev/v1beta1", "ClusterTask", "init")) //
                .addNewParam() //
                .withName("data") //
                // TODO: Do not pass the JSON, but instead we should pass just coordinates
                .withNewValue(componentJson) //
                .endParam() //
                .withWorkspaces(pipelineWsBinding) //
                .build();

        tasks.add(initTask);

        // Prepare main task
        PipelineTask buildTask = new PipelineTaskBuilder() //
                .withName(component.getMetadata().getName()) //
                .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", builder.getSpec().getTask())) //
                .withWorkspaces(stageWsBinding, pipelineWsBinding) //
                .withRunAfter("init") //
                .build();

        tasks.add(buildTask);

        // // TODO: Remove, only for now
        // PipelineTask s3Task = new PipelineTaskBuilder() //
        // .withName("s3") //
        // .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", "s3")) //
        // .addNewParam() //
        // .withName("component") //
        // .withNewValue(componentJson) //
        // .endParam() //
        // .withWorkspaces(resultsWsBinding, sharedWsBinding) //
        // .withRunAfter(component.getMetadata().getName()) // TODO: hardcoded
        // .build();

        // tasks.add(s3Task);

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName("ws") //
                .withDescription("Main workspace") //
                .build();

        // Pipeline result is the result of the main task executed
        PipelineResult pipelineResult = new PipelineResultBuilder() //
                .withName("data") //
                .withValue("$(tasks.".concat(component.getMetadata().getName()).concat(".results.data)")) //
                .build();

        // Add any useful/required labels
        Map<String, String> labels = new HashMap<>();
        labels.put(Resource.COMPONENT.getLabel(), component.getMetadata().getName());

        // Make sure the Pipeline is owned by the Component
        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(component.getApiVersion()) //
                .withKind(component.getKind()) //
                .withName(component.getMetadata().getName()) //
                .withUid(component.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        // Define the Pipeline itself
        Pipeline pipeline = new PipelineBuilder() //
                .withNewMetadata() //
                .withOwnerReferences(ownerReference) //
                .withLabels(labels) //
                .withName(component.getMetadata().getName()) //
                .endMetadata() //
                .withNewSpec() //
                .withWorkspaces(workspaceMain) //
                .addAllToTasks(tasks) //
                .withResults(pipelineResult) //
                .endSpec() //
                .build();

        return tektonClient.v1beta1().pipelines().inNamespace(component.getMetadata().getNamespace())
                .createOrReplace(pipeline);
    }

}
