package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.AbstractStage;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Pipeline.PipelineSpec.StageEntry;
import com.redhat.agogos.v1alpha1.Pipeline.PipelineSpec.StageReference;
import com.redhat.agogos.v1alpha1.WorkspaceMapping;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclarationBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Controller
public class PipelineController implements ResourceController<Pipeline> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineController.class);

    @Inject
    TektonClient tektonClient;

    @Inject
    AgogosClient agogosClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public DeleteControl deleteResource(Pipeline resource, Context<Pipeline> context) {
        LOG.info("Pipeline '{}' deleted", resource.getNamespacedName());
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<Pipeline> createOrUpdateResource(Pipeline pipeline, Context<Pipeline> context) {
        LOG.info("Pipeline '{}' modified", pipeline.getNamespacedName());

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding pipelineWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME) //
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

        // groupClient.getByName(pipeline.getSpec().getGroup()); TODO

        for (StageEntry stageEntry : pipeline.getSpec().getStages()) {
            StageReference stageRef = stageEntry.getStageRef();

            LOG.debug("Processing {} '{}' ", stageRef.getKind(), stageRef.getName());

            AbstractStage stage = null;
            String taskType = "Task";

            switch (stageRef.getKind()) {
                case "Stage":
                    stage = agogosClient.v1alpha1().stages().inNamespace(pipeline.getMetadata().getNamespace())
                            .withName(stageRef.getName()).get();
                    break;
                case "ClusterStage":
                    stage = agogosClient.v1alpha1().clusterstages().withName(stageRef.getName()).get();
                    taskType = "ClusterTask";
                    break;
                default:
                    throw new ApplicationException("Invalid Stage kind: {}", stageRef.getKind());
            }

            if (stage == null) {
                throw new MissingResourceException("Selected {} '{}' is not available in the system",
                        stageRef.getKind(), stageRef.getName());
            }

            String stageConfig;

            // Convert Component metadata to JSON
            try {
                LOG.debug("Converting Stage '{}' configuration to JSON", stageRef.getName());
                stageConfig = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stageEntry.getConfig());
            } catch (JsonProcessingException e) {
                LOG.debug("Unable to convert Stage '{}' configuration to JSON", stageRef.getName());
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
                    .withTaskRef(new TaskRefBuilder().withApiVersion("tekton.dev/v1beta1").withKind(taskType)
                            .withName(stage.getSpec().getTaskRef().getName()).build()) //
                    .addNewParam() //
                    .withName("config") //
                    .withNewValue(stageConfig) //
                    .endParam() //
                    .withWorkspaces(stageWsBinding, pipelineWsBinding) //
                    .build();

            // set additional task property
            if (stageEntry.getRunAfter() != null) {
                task.setRunAfter(stageEntry.getRunAfter());
            }

            tasks.add(task);
        }

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName("ws") //
                .withDescription("Main workspace") //
                .build();

        // Define the Pipeline itself
        io.fabric8.tekton.pipeline.v1beta1.Pipeline tektonPipeline = new PipelineBuilder() //
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
