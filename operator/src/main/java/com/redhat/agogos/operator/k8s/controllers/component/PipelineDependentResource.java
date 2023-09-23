package com.redhat.agogos.operator.k8s.controllers.component;

import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec.BuilderRef;
import com.redhat.agogos.core.v1alpha1.ComponentHandlerSpec;
import com.redhat.agogos.core.v1alpha1.Handler;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PipelineDependentResource extends AbstractDependentResource<Pipeline, Component> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineDependentResource.class);

    private static final String BUILD_PIPELINE_BUILDER_TASK_NAME = "build";
    private static final String BUILD_PIPELINE_DEFAULT_TASK_WORKSPACE_NAME = "output";
    private static final String BUILD_PIPELINE_SOURCE_TASK_WORKSPACE_NAME = "output";

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

        List<PipelineTask> tasks = new ArrayList<>();

        // Add any pre Handlers
        prepareHandlerTasks(component, component.getSpec().getPre(), tasks);

        // Finally add the Builder task
        prepareBuilderTask(component, tasks);

        // Add any post Handlers
        prepareHandlerTasks(component, component.getSpec().getPost(), tasks);

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder()
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withDescription("Main workspace used in the the build pipeline")
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

        ParamSpec param = new ParamSpecBuilder()
                .withName("component")
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
                .withParams(param)
                .withWorkspaces(workspaceMain)
                .addAllToTasks(tasks)
                .withResults(result)
                .endSpec()
                .build();

        LOG.debug("New Pipeline '{}' created for Component '{}'", pipeline.getMetadata().getName(), component.getFullName());
        return pipeline;
    }

    private void prepareHandlerTasks(Component component, List<ComponentHandlerSpec> handlers, List<PipelineTask> tasks) {
        handlers.stream().forEach(handlerSpec -> {
            String handlerName = handlerSpec.getHandlerRef().getName();

            Handler handler = kubernetesFacade.get(Handler.class, component.getMetadata().getNamespace(), handlerName);
            PipelineTaskBuilder pipelineTaskBuilder = new PipelineTaskBuilder()
                    .withName(handlerSpec.getHandlerRef().getName())
                    .withTaskRef(handler.getSpec().getTaskRef())
                    .withParams(handlerSpec.getParams())
                    .withWorkspaces(workspaceBindings(handler.getSpec().getWorkspaces()));

            if (tasks.size() > 1) {
                pipelineTaskBuilder.withRunAfter(tasks.get(tasks.size() - 1).getName());
            }

            PipelineTask pipelineTask = pipelineTaskBuilder.build();

            tasks.add(pipelineTask);
        });
    }

    private void prepareBuilderTask(Component component, List<PipelineTask> tasks) {
        BuilderRef builderRef = component.getSpec().getBuild().getBuilderRef();
        String name = builderRef.getName();
        String namespace = agogosEnv.getRunningNamespace(builderRef);

        Builder builder = kubernetesFacade.get(Builder.class, namespace, name);
        if (builder == null) {
            throw new MissingResourceException("Selected Builder '{}' is not available in the namespace '{}'",
                    name, namespace);
        }

        PipelineTask pipelineTask = new PipelineTaskBuilder()
                .withName(BUILD_PIPELINE_BUILDER_TASK_NAME)
                .withTaskRef(builder.getSpec().getTaskRef())
                .withParams(component.getSpec().getBuild().getParams())
                .addNewParam()
                .withName("component")
                .withNewValue("$(params.component)")
                .endParam()
                .withWorkspaces(workspaceBindings(builder.getSpec().getWorkspaces()))
                .build();

        tasks.add(pipelineTask);
    }

    /**
     * <p>
     * Handle declared workspace binding on the resource. If there are any provided - these will be iterated over and required
     * workspaces will be bound.
     * </p>
     * 
     * <p>
     * In case no bindings are provided, default binding will be applied.
     * </p>
     * 
     * @param mappings
     * @param defaultWorkspace
     * @return
     */
    private List<WorkspacePipelineTaskBinding> workspaceBindings(List<WorkspaceMapping> mappings) {
        List<WorkspacePipelineTaskBinding> workspaceBindings = new ArrayList<>();

        if (mappings == null || mappings.isEmpty()) {
            WorkspacePipelineTaskBinding defaultTaskBinding = new WorkspacePipelineTaskBindingBuilder()
                    .withName(BUILD_PIPELINE_DEFAULT_TASK_WORKSPACE_NAME)
                    .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                    .build();

            workspaceBindings.add(defaultTaskBinding);
        } else {
            mappings.forEach(workspaceMapping -> {
                String targetWorkspaceName = BUILD_PIPELINE_SOURCE_TASK_WORKSPACE_NAME;

                // Handle workspace mappings, if provided
                if (workspaceMapping.getName() != null) {
                    targetWorkspaceName = workspaceMapping.getName();
                }

                WorkspacePipelineTaskBinding pipelineTaskBinding = new WorkspacePipelineTaskBindingBuilder()
                        .withName(targetWorkspaceName)
                        .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                        .withSubPath(workspaceMapping.getSubPath())
                        .build();

                workspaceBindings.add(pipelineTaskBinding);
            });
        }

        return workspaceBindings;
    }
}
