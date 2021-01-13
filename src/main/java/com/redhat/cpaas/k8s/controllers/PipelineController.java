package com.redhat.cpaas.k8s.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.model.PipelineResource;
import com.redhat.cpaas.k8s.model.PipelineResource.PipelineSpec.StageReference;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineBuilder;
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

@ApplicationScoped
@Controller(crdName = "pipelines.cpaas.redhat.com")
public class PipelineController implements ResourceController<PipelineResource> {

    private static final Logger LOG = Logger.getLogger(PipelineController.class);

    @Inject
    TektonClient tektonClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public DeleteControl deleteResource(PipelineResource resource, Context<PipelineResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<PipelineResource> createOrUpdateResource(PipelineResource pipeline,
            Context<PipelineResource> context) {
        LOG.infov("Pipeline ''{0}'' modified", pipeline.getMetadata().getName());

        // Prepare workspace for main task to store results
        WorkspacePipelineTaskBinding resultsWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("results") //
                .withWorkspace("ws") //
                .withSubPath("results") //
                .build();

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding sharedWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("shared") //
                .withWorkspace("ws") //
                .withSubPath("shared") //
                .build();

        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(pipeline.getApiVersion()) //
                .withKind(pipeline.getKind()) //
                .withName(pipeline.getMetadata().getName()) //
                .withUid(pipeline.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        List<PipelineTask> tasks = new ArrayList<>();

        for (StageReference stageRef : pipeline.getSpec().getStages()) {
            String stageConfig;

            // Convert Component metadata to JSON
            try {
                stageConfig = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stageRef.getConfig());
                System.out.println(stageConfig);
            } catch (JsonProcessingException e) {

                return UpdateControl.noUpdate();

                // throw new ApplicationException(String
                // .format("Internal error; could not serialize configuration for stage '%s'",
                // stageRef.getName()), e);
            }

            // Prepare task
            PipelineTask task = new PipelineTaskBuilder() //
                    .withName(stageRef.getName()) //
                    .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", stageRef.getName())) //
                    .addNewParam() //
                    .withName("config") //
                    .withNewValue(stageConfig) //
                    .endParam() //
                    .withWorkspaces(resultsWsBinding, sharedWsBinding) //
                    .build();

            tasks.add(task);
        }

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName("ws") //
                .withDescription("Main workspace") //
                .build();

        // Define the Pipeline itself
        Pipeline tektonPipeline = new PipelineBuilder() //
                .withNewMetadata() //
                .withOwnerReferences(ownerReference) //
                // .withLabels(labels) //
                .withName(pipeline.getMetadata().getName())//
                .endMetadata() //
                .withNewSpec() //
                .withWorkspaces(workspaceMain) //
                .addAllToTasks(tasks) //
                // .withResults(pipelineResult) //
                .endSpec() //
                .build();

        tektonClient.v1beta1().pipelines().inNamespace(pipeline.getMetadata().getNamespace())
                .createOrReplace(tektonPipeline);

        return UpdateControl.noUpdate();
    }
}
