package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.k8s.client.PipelineClient;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Status;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRef;
import io.fabric8.tekton.pipeline.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ComponentController implements ResourceController<Component> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentController.class);

    @Inject
    PipelineClient componentResourceClient;

    @Inject
    AgogosClient agogosClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TektonClient tektonClient;

    /**
     * <p>
     * Method triggered when a {@link Component} is removed from the
     * cluster.
     * </p>
     * 
     * @param component {@link Component}
     * @param context {@link Context}
     * @return {@link DeleteControl}
     */
    @Override
    public DeleteControl deleteResource(Component component, Context<Component> context) {
        LOG.info("Removing component '{}'", component.getNamespacedName());
        return DeleteControl.DEFAULT_DELETE;
    }

    /**
     * <p>
     * Main method that is triggered when a change on the {@link Component}
     * object is detected.
     * </p>
     * 
     * @param component {@link Component}
     * @param context {@link Context}
     * @return {@link UpdateControl}
     */
    @Override
    public UpdateControl<Component> createOrUpdateResource(Component component,
            Context<Component> context) {

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
     * Updates {@link Component.ComponentStatus} of the particular
     * {@link Component}.
     * <p/>
     * 
     * 
     * @param component {@link Component} object
     * @param status One of available statuses
     * @param reason Description of the reason for last status change
     */
    private void setStatus(final Component component, final ResourceStatus status, final String reason) {
        Status componentStatus = component.getStatus();

        componentStatus.setStatus(String.valueOf(status));
        componentStatus.setReason(reason);
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    }

    /**
     * <p>
     * Creates or updates Tekton pipeline based on the {@link Component}
     * data passed and sets the status subresource on {@link Component}
     * depending on the outcome of the pipeline update.
     * </p>
     */
    private UpdateControl<Component> onResourceUpdate(Component component,
            Context<Component> context) {
        LOG.info("Component '{}' modified", component.getFullName());

        try {
            LOG.debug("Preparing pipeline for Component '{}'", component.getNamespacedName());

            this.updateTektonBuildPipeline(component);

            setStatus(component, ResourceStatus.Ready, "Component is ready");

            LOG.info("Pipeline for Component '{}' updated", component.getNamespacedName());
        } catch (ApplicationException e) {
            LOG.error("Error occurred while creating pipeline for component '{}'", component.getNamespacedName(), e);

            setStatus(component, ResourceStatus.Failed, "Could not create Component: " + e.getMessage());
        }

        return UpdateControl.updateStatusSubResource(component);
    }

    /**
     * <p>
     * Creates or updates Tekton pipeline based on the {@link Component}
     * data passed.
     * </p>
     * 
     * @param component {@link Component} to create the pipeline for
     * @return {@link Pipeline} object for the updated pipeline
     * @throws ApplicationException in case the pipeline cannot be updated
     */
    private Pipeline updateTektonBuildPipeline(Component component) throws ApplicationException {
        String componentJson;

        // Convert Component metadata to JSON
        // TODO: Remove this
        try {
            componentJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(component.toEasyMap());
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Internal error; could not serialize component '{}'",
                    component.getNamespacedName(), e);
        }

        Builder builder = agogosClient.v1alpha1().builders().withName(component.getSpec().getBuilderRef().get("name")).get();

        if (builder == null) {
            throw new MissingResourceException("Selected Builder '{}' is not available in the system",
                    component.getSpec().getBuilderRef().get("name"));
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
                .withTaskRef(new TaskRefBuilder().withName("init").withApiVersion("tekton.dev/v1beta1").withKind("ClusterTask")
                        .build()) //
                .addNewParam() //
                .withName("data") //
                // TODO: Do not pass the JSON, but instead we should pass just coordinates
                .withNewValue(componentJson) //
                .endParam() //
                .addNewParam() //
                .withName("name") //
                .withNewValue(component.getMetadata().getName()) //
                .endParam() //
                .addNewParam() //
                .withName("kind") //
                .withNewValue(component.getKind()) //
                .endParam() //
                .addNewParam() //
                .withName("apiversion") //
                .withNewValue(component.getVersion()) //
                .endParam() //
                .withWorkspaces(pipelineWsBinding) //
                .build();

        tasks.add(initTask);

        TaskRef buildTaskRef = new TaskRefBuilder().withApiVersion(HasMetadata.getApiVersion(Task.class))
                .withKind(builder.getSpec().getTaskRef().getKind()).withName(builder.getSpec().getTaskRef().getName()).build();

        // Prepare main task
        PipelineTask buildTask = new PipelineTaskBuilder() //
                .withName("builder") //
                .withTaskRef(buildTaskRef)
                .withWorkspaces(stageWsBinding, pipelineWsBinding) //
                .withRunAfter("init") //
                .build();

        tasks.add(buildTask);

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName("ws") //
                .withDescription("Main workspace") //
                .build();

        // Pipeline result is the result of the main task executed
        PipelineResult pipelineResult = new PipelineResultBuilder() //
                .withName("data") //
                .withValue("$(tasks.builder.results.data)") //
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
