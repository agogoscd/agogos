package com.redhat.agogos.operator.k8s.controllers.pipeline;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.v1alpha1.AbstractStage;
import com.redhat.agogos.core.v1alpha1.ClusterStage;
import com.redhat.agogos.core.v1alpha1.Pipeline.PipelineSpec.StageEntry;
import com.redhat.agogos.core.v1alpha1.Pipeline.PipelineSpec.StageReference;
import com.redhat.agogos.core.v1alpha1.WorkspaceMapping;
import com.redhat.agogos.operator.k8s.controllers.AbstractDependentResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PipelineDependentResource
        extends AbstractDependentResource<Pipeline, com.redhat.agogos.core.v1alpha1.Pipeline> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineDependentResource.class);

    public PipelineDependentResource() {
        super(Pipeline.class);
    }

    @Override
    protected Pipeline desired(com.redhat.agogos.core.v1alpha1.Pipeline agogos,
            Context<com.redhat.agogos.core.v1alpha1.Pipeline> context) {
        Pipeline pipeline = new Pipeline();

        Optional<Pipeline> optional = context.getSecondaryResource(Pipeline.class);
        if (!optional.isEmpty()) {
            LOG.debug("Agogos Pipeline '{}', using existing Tekton Pipeline", agogos.getFullName());
            pipeline = optional.get();
        } else {
            LOG.debug("Agogos Pipeline '{}', creating new Tekton Pipeline", agogos.getFullName());
        }

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding pipelineWsBinding = new WorkspacePipelineTaskBindingBuilder()
                .withName("pipeline")
                .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withSubPath("pipeline")
                .build();

        OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion(agogos.getApiVersion())
                .withKind(agogos.getKind())
                .withName(agogos.getMetadata().getName())
                .withUid(agogos.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .withController(true)
                .build();

        List<PipelineTask> tasks = new ArrayList<>();

        for (StageEntry stageEntry : agogos.getSpec().getStages()) {
            StageReference stageRef = stageEntry.getStageRef();

            LOG.debug("Processing {} '{}' ", stageRef.getKind(), stageRef.getName());

            AbstractStage stage = null;
            String taskType = "Task";

            switch (stageRef.getKind()) {
                case "Stage":
                    stage = kubernetesFacade.get(AbstractStage.class, agogos.getMetadata().getNamespace(), stageRef.getName());
                    break;
                case "ClusterStage":
                    stage = kubernetesFacade.get(ClusterStage.class, stageRef.getName());
                    taskType = "ClusterTask";
                    break;
                default:
                    throw new ApplicationException("Invalid Stage kind: {}", stageRef.getKind());
            }

            if (stage == null) {
                throw new MissingResourceException("Selected {} '{}' is not available in the system",
                        stageRef.getKind(), stageRef.getName());
            }

            String stageConfig = "{}";

            // Convert Component metadata to JSON
            LOG.debug("Converting Stage '{}' configuration to JSON", stageRef.getName());
            stageConfig = objectMapper.asJson(stageEntry.getConfig());

            // Prepare workspace for main task to store results
            WorkspacePipelineTaskBinding stageWsBinding = new WorkspacePipelineTaskBindingBuilder()
                    .withName("stage")
                    .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                    .withSubPath(String.format("pipeline/%s", stageRef.getName()))
                    .build();

            // Prepare task
            PipelineTask task = new PipelineTaskBuilder()
                    .withName(stageRef.getName())
                    .withTaskRef(new TaskRefBuilder()
                            .withApiVersion("") // AGOGOS-96
                            .withKind(taskType)
                            .withName(stage.getSpec().getTaskRef().getName())
                            .build())
                    .addNewParam()
                    .withName("config")
                    .withNewValue(stageConfig)
                    .endParam()
                    .withWorkspaces(stageWsBinding, pipelineWsBinding)
                    .build();

            // set additional task property
            if (stageEntry.getRunAfter() != null) {
                task.setRunAfter(stageEntry.getRunAfter());
            }

            tasks.add(task);
        }

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder()
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withDescription("Main workspace")
                .build();

        // Define the Pipeline itself
        pipeline = new PipelineBuilder(pipeline)
                .withNewMetadata()
                .withOwnerReferences(ownerReference)
                // .withLabels(labels)
                .withName(agogos.getMetadata().getName())
                .withNamespace(agogos.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withWorkspaces(workspaceMain)
                .addAllToTasks(tasks)
                // .withResults(pipelineResult)
                .endSpec()
                .build();

        LOG.debug("New Tekton Pipeline '{}' created for Agogos Pipeline '{}",
                pipeline.getMetadata().getName(), agogos.getFullName());
        return pipeline;
    }
}
