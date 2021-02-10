package com.redhat.cpaas.k8s.controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.client.StageResourceClient;
import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.k8s.errors.ApplicationException;
import com.redhat.cpaas.k8s.errors.MissingResourceException;
import com.redhat.cpaas.k8s.model.AbstractStage.Phase;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.ComponentResource.ComponentStatus;
import com.redhat.cpaas.k8s.model.ComponentResource.Status;
import com.redhat.cpaas.k8s.model.StageResource;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
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

@Controller
public class ComponentController implements ResourceController<ComponentResource> {

    private static final Logger LOG = Logger.getLogger(ComponentController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ComponentResourceClient componentResourceClient;

    @Inject
    TektonResourceClient tektonResourceClient;

    @Inject
    StageResourceClient stageResourceClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TektonClient tektonClient;

    /**
     * Updates {@link ComponentResource.ComponentStatus} of the particular
     * {@link ComponentResource}.
     * 
     * This is useful when the are hooks executed which influence the ability of
     * usage of the Component.
     * 
     * @param component {@link ComponentResource} object
     * @param status    One of available statuses
     * @param reason    Description of the reason for last status change
     */
    private void setStatus(final ComponentResource component, final Status status, final String reason) {
        ComponentStatus componentStatus = component.getStatus();

        componentStatus.setStatus(String.valueOf(status));
        componentStatus.setReason(reason);
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    }

    private void createPipeline(ComponentResource component) {
        try {
            LOG.debugv("Preparing pipeline for component ''{0}''", component.getMetadata().getName());

            this.createBuildPipeline(component);

            setStatus(component, Status.Initializing, "Preparing pipeline");

            LOG.infov("Pipeline for component ''{0}'' updated", component.getMetadata().getName());
        } catch (ApplicationException e) {
            LOG.errorv(e, "Error occurred while creating pipeline for component ''{0}''",
                    component.getMetadata().getName());

            setStatus(component, Status.Failed, "Could not create component pipeline");

        }
    }

    @Override
    public DeleteControl deleteResource(ComponentResource component, Context<ComponentResource> context) {
        LOG.infov("Removing component ''{0}''", component.getMetadata().getName());
        return DeleteControl.DEFAULT_DELETE;
    }

    public UpdateControl<ComponentResource> onResourceUpdate(ComponentResource component,
            Context<ComponentResource> context) {
        LOG.infov("Component ''{0}'' modified", component.getMetadata().getName());

        // TODO: Handle component updates

        switch (Status.valueOf(component.getStatus().getStatus())) {
            case New:
                createPipeline(component);
                // TODO
                // Add handling of additional hooks required to be run before the
                // component can be built

                setStatus(component, Status.Ready, "");
                return UpdateControl.updateStatusSubResource(component);
            // case Initializing:
            // // TODO
            // // This is not how it should be, we need to find a way to watch resources to
            // // make the component ready when it is ready
            // setStatus(component, Status.Ready, "");
            // return UpdateControl.updateStatusSubResource(component);
            default:
                break;
        }

        return UpdateControl.noUpdate();
    }

    public UpdateControl<ComponentResource> onEvent(ComponentResource resource, Context<ComponentResource> context) {
        return UpdateControl.updateStatusSubResource(resource);
    }

    @Override
    public UpdateControl<ComponentResource> createOrUpdateResource(ComponentResource resource,
            Context<ComponentResource> context) {
        final var customResourceEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
        if (customResourceEvent.isPresent()) {
            return onResourceUpdate(resource, context);
        }
        return onEvent(resource, context);
    }

    private Pipeline createBuildPipeline(ComponentResource component) throws ApplicationException {
        // Find the builder defined by the component
        // TODO: This validation should be done as part of ValidatingAdmissionWebhook
        StageResource builder = stageResourceClient.getByName(component.getSpec().getBuilder(), Phase.BUILD);

        if (builder == null) {
            throw new MissingResourceException(String.format("Selected builder '%s' is not registered in the system",
                    component.getSpec().getBuilder()));
        }

        String componentJson;

        // Convert Component metadata to JSON
        try {
            componentJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(component.toEasyMap());
        } catch (JsonProcessingException e) {
            throw new ApplicationException(String.format("Internal error; could not serialize component '%s'",
                    component.getMetadata().getName()), e);
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
                .withNewValue(componentJson) //
                .endParam() //
                .withWorkspaces(pipelineWsBinding) //
                .build();

        tasks.add(initTask);

        // TODO: Make pre and post-tasks configurable?
        // Prepare main task
        PipelineTask buildTask = new PipelineTaskBuilder() //
                .withName(component.getMetadata().getName()) //
                .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", component.getSpec().getBuilder())) //
                .addNewParam() //
                .withName("component") //
                .withNewValue(componentJson) //
                .endParam() //
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
        labels.put("cpaas.redhat.com/component", component.getMetadata().getName());

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