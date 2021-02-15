package com.redhat.cpaas.k8s.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.client.ComponentGroupResourceClient;
import com.redhat.cpaas.v1alpha1.PipelineResource;
import com.redhat.cpaas.v1alpha1.PipelineResource.PipelineSpec.StageReference;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class PipelineController implements ResourceController<PipelineResource> {

    private static final Logger LOG = LoggerFactory.getLogger( PipelineController.class);

    @Inject
    TektonClient tektonClient;

    @Inject
    ComponentGroupResourceClient componentGroupResourceClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public DeleteControl deleteResource(PipelineResource resource, Context<PipelineResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<PipelineResource> createOrUpdateResource(PipelineResource pipeline,
            Context<PipelineResource> context) {
        LOG.info("Pipeline '{}' modified", pipeline.getMetadata().getName());

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding pipelineWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("pipeline") //
                .withWorkspace("ws") //
                .withSubPath("pipeline") //
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

        componentGroupResourceClient.getByName(pipeline.getSpec().getGroup());

        for (StageReference stageRef : pipeline.getSpec().getStages()) {
            String stageConfig;

            // Convert Component metadata to JSON
            try {
                stageConfig = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stageRef.getConfig());
            } catch (JsonProcessingException e) {

                return UpdateControl.noUpdate();
            }

            // Prepare workspace for main task to store results
            WorkspacePipelineTaskBinding stageWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                    .withName("stage") //
                    .withWorkspace("ws") //
                    .withSubPath(String.format("pipeline/%s", stageRef.getName())) //
                    .build();

            // Prepare task
            PipelineTask task = new PipelineTaskBuilder() //
                    .withName(stageRef.getName()) //
                    .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", stageRef.getName())) //
                    .addNewParam() //
                    .withName("config") //
                    .withNewValue(stageConfig) //
                    .endParam() //
                    .withWorkspaces(stageWsBinding, pipelineWsBinding) //
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
