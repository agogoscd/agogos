package com.redhat.agogos.operator.k8s.controllers;

import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.StageEntry;
import com.redhat.agogos.core.v1alpha1.StageEntry.StageReference;
import com.redhat.agogos.core.v1alpha1.WorkspaceMapping;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@KubernetesDependent
public abstract class AbstractDependentResource<R extends HasMetadata, P extends HasMetadata>
        extends CRUDKubernetesDependentResource<R, P> {

    private Logger LOG = LoggerFactory.getLogger(AbstractDependentResource.class);

    public AbstractDependentResource(Class<R> resourceType) {
        super(resourceType);
    }

    @Inject
    protected AgogosEnvironment agogosEnv;

    @Inject
    protected KubernetesFacade kubernetesFacade;

    @Inject
    protected KubernetesSerialization objectMapper;

    protected AgogosResource<?, ?> parentResource(P resource) {
        throw new ApplicationException("No implementation of parentResource for '{}'", resource.getKind());
    }

    protected List<PipelineTask> createTasks(List<StageEntry> entries, WorkspacePipelineTaskBinding pipelineWsBinding,
            String namespace) {

        List<PipelineTask> tasks = new ArrayList<>();

        for (StageEntry stageEntry : entries) {
            StageReference stageRef = stageEntry.getStageRef();
            String name = stageRef.getName();

            Stage stage = lookupStage(stageRef, namespace);
            String stageConfig = "{}";

            // Convert Component metadata to JSON
            LOG.debug("Converting Stage '{}' configuration to JSON", name);
            stageConfig = objectMapper.asJson(stageEntry.getConfig());

            // Prepare workspace for main task to store results
            WorkspacePipelineTaskBinding stageWsBinding = new WorkspacePipelineTaskBindingBuilder()
                    .withName("stage")
                    .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                    .withSubPath(String.format("pipeline/%s", name))
                    .build();

            // Prepare task
            PipelineTask task = new PipelineTaskBuilder()
                    .withName(name)
                    .withTaskRef(new TaskRefBuilder()
                            .withApiVersion("") // AGOGOS-96
                            .withKind(stage.getSpec().getTaskRef().getKind())
                            .withName(stage.getSpec().getTaskRef().getName())
                            .withResolver(stage.getSpec().getTaskRef().getResolver())
                            .withParams(stage.getSpec().getTaskRef().getParams())
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
        return tasks;
    }

    protected Stage lookupStage(StageReference stageRef, String pnamespace) {
        String name = stageRef.getName();

        Stage stage = kubernetesFacade.get(Stage.class, pnamespace, name);
        if (stage == null) {
            String namespace = agogosEnv.getRunningNamespace(stageRef);
            stage = kubernetesFacade.get(Stage.class, namespace, name);
            if (stage == null) {
                throw new MissingResourceException("Selected Stage '{}' is not available in namespaces '{}' or '{}'",
                        name, pnamespace, namespace);
            }
        }

        LOG.debug("Stage '{}' found in namespace '{}'", name, stage.getMetadata().getNamespace());
        return stage;
    }
}
