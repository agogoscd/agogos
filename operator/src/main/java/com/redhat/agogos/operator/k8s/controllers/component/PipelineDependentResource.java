package com.redhat.agogos.operator.k8s.controllers.component;

import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec.BuilderRef;
import com.redhat.agogos.core.v1alpha1.WorkspaceMapping;
import com.redhat.agogos.operator.k8s.controllers.AbstractDependentResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpec;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineResult;
import io.fabric8.tekton.pipeline.v1beta1.PipelineResultBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclarationBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PipelineDependentResource extends AbstractDependentResource<Pipeline, Component> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineDependentResource.class);

    private static final String BUILD_PIPELINE_BUILDER_TASK_NAME = "build";

    @Inject
    AgogosEnvironment agogosEnv;

    public PipelineDependentResource() {
        super(Pipeline.class);
    }

    @Override
    protected Pipeline desired(Component component, Context<Component> context) {
        Pipeline pipeline = new Pipeline();

        Optional<Pipeline> optional = context.getSecondaryResource(Pipeline.class);
        if (!optional.isEmpty()) {
            LOG.debug("Component '{}', using existing Pipeline", component.getFullName());
            pipeline = optional.get();
        } else {
            LOG.debug("Component '{}', creating new Pipeline", component.getFullName());
        }

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding pipelineWsBinding = new WorkspacePipelineTaskBindingBuilder()
                .withName("pipeline")
                .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withSubPath("pipeline")
                .build();

        // Create all the pre, builder, and post tasks.
        List<PipelineTask> preTasks = createTasks(component.getSpec().getPre(), pipelineWsBinding,
                component.getMetadata().getNamespace());
        PipelineTask builderTask = createBuilderTask(component, pipelineWsBinding);
        List<PipelineTask> postTasks = createTasks(component.getSpec().getPost(), pipelineWsBinding,
                component.getMetadata().getNamespace());

        // Set runafter values so all pre tasks run before the build, and all post tasks after.
        builderTask.setRunAfter(preTasks.stream().map(t -> t.getName()).collect(Collectors.toList()));
        postTasks.stream().forEach(task -> {
            task.getRunAfter().add(builderTask.getName());
        });

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder()
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withDescription("Main workspace")
                .build();

        // Add any useful/required labels
        Map<String, String> labels = new HashMap<>();
        labels.put(Label.RESOURCE.toString(), Resource.COMPONENT.toString().toLowerCase());
        labels.put(Label.NAME.toString(), component.getMetadata().getName());

        // Make sure the Pipeline is owned by the Component
        OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion(component.getApiVersion())
                .withKind(component.getKind())
                .withName(component.getMetadata().getName())
                .withUid(component.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .withController(true)
                .build();

        ParamSpec componentParam = new ParamSpecBuilder()
                .withName("component")
                .withType("string")
                .build();

        ParamSpec paramsParam = new ParamSpecBuilder()
                .withName("params")
                .withType("string")
                .build();

        PipelineResult result = new PipelineResultBuilder()
                .withDescription("Builder results")
                .withName("output")
                .withNewValue("$(tasks.build.results.data)")
                .build();

        // Define the Pipeline itself
        pipeline = new PipelineBuilder(pipeline)
                .withNewMetadata()
                .withOwnerReferences(ownerReference)
                .withLabels(labels)
                .withName(component.getMetadata().getName())
                .withNamespace(component.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withParams(componentParam, paramsParam)
                .withWorkspaces(workspaceMain)
                .addAllToTasks(preTasks)
                .addToTasks(builderTask)
                .addAllToTasks(postTasks)
                .withResults(result)
                .endSpec()
                .build();

        LOG.debug("New Pipeline '{}' created for Component '{}'", pipeline.getMetadata().getName(), component.getFullName());
        return pipeline;
    }

    public PipelineTask createBuilderTask(Component component, WorkspacePipelineTaskBinding pipelineWsBinding) {

        // Prepare workspace for builder task to store results
        WorkspacePipelineTaskBinding stageWsBinding = new WorkspacePipelineTaskBindingBuilder()
                .withName("stage")
                .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withSubPath(String.format("pipeline/%s", BUILD_PIPELINE_BUILDER_TASK_NAME))
                .build();

        Builder builder = lookupBuilder(component);
        PipelineTask pipelineTask = new PipelineTaskBuilder()
                .withName(BUILD_PIPELINE_BUILDER_TASK_NAME)
                .withTaskRef(builder.getSpec().getTaskRef())
                .addNewParam()
                .withName("params")
                .withNewValue("$(params.params)")
                .endParam()
                .addNewParam()
                .withName("component")
                .withNewValue("$(params.component)")
                .endParam()
                .withWorkspaces(stageWsBinding, pipelineWsBinding)
                .build();

        return pipelineTask;
    }

    private Builder lookupBuilder(Component component) {
        String cnamespace = component.getMetadata().getNamespace();
        BuilderRef builderRef = component.getSpec().getBuild().getBuilderRef();
        String name = builderRef.getName();

        Builder builder = kubernetesFacade.get(Builder.class, cnamespace, name);
        if (builder == null) {
            String namespace = agogosEnv.getRunningNamespace(builderRef);
            builder = kubernetesFacade.get(Builder.class, namespace, name);
            if (builder == null) {
                throw new MissingResourceException("Selected Builder '{}' is not available in namespaces '{}' or '{}'",
                        name, cnamespace, namespace);
            }
        }

        LOG.debug("Builder '{}' found in namespace '{}'", name, builder.getMetadata().getNamespace());
        return builder;
    }
}
