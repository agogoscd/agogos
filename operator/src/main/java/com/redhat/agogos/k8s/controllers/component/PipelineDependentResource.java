package com.redhat.agogos.k8s.controllers.component;

import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.controllers.AbstractDependentResource;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentHandlerSpec;
import com.redhat.agogos.v1alpha1.Handler;
import com.redhat.agogos.v1alpha1.WorkspaceMapping;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpec;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamValueBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclarationBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRef;
import io.fabric8.tekton.pipeline.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
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
        labels.put(Resource.COMPONENT.getLabel(), component.getMetadata().getName());

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
                .endSpec()
                .build();

        LOG.debug("New Pipeline '{}' created for Component '{}'", pipeline.getMetadata().getName(), component.getFullName());
        return pipeline;
    }

    private void prepareHandlerTasks(Component component, List<ComponentHandlerSpec> handlers, List<PipelineTask> tasks) {
        handlers.stream().forEach(handlerSpec -> {
            String handlerName = handlerSpec.getHandlerRef().getName();

            Handler handler = agogosClient.v1alpha1().handlers().inNamespace(component.getMetadata().getNamespace())
                    .withName(handlerName).get();

            Task handlerTask = tektonClient.v1beta1().tasks().inNamespace(component.getMetadata().getNamespace())
                    .withName(handler.getSpec().getTaskRef().getName()).get();

            PipelineTaskBuilder pipelineTaskBuilder = new PipelineTaskBuilder()
                    .withName(handlerName)
                    .withTaskRef(
                            new TaskRefBuilder().withName(handlerTask.getMetadata().getName())
                                    .withApiVersion("") // AGOGOS-96
                                    .withKind(handlerTask.getKind())
                                    .build())
                    .withWorkspaces(workspaceBindings(handler.getSpec().getWorkspaces()));

            if (tasks.size() > 1) {
                pipelineTaskBuilder.withRunAfter(tasks.get(tasks.size() - 1).getName());
            }

            addParams(pipelineTaskBuilder, handlerTask.getSpec().getParams(), handlerSpec.getParams());

            PipelineTask pipelineTask = pipelineTaskBuilder.build();

            tasks.add(pipelineTask);
        });
    }

    private void prepareBuilderTask(Component component, List<PipelineTask> tasks) {
        String builderName = component.getSpec().getBuild().getBuilderRef().getName();

        Builder builder = agogosClient.v1alpha1().builders().withName(builderName)
                .get();

        if (builder == null) {
            throw new MissingResourceException("Selected Builder '{}' is not available in the system",
                    builderName);
        }

        List<ParamSpec> params = null;
        com.redhat.agogos.v1alpha1.TaskRef taskRef = builder.getSpec().getTaskRef();
        if ("ClusterTask".equals(taskRef.getKind())) {
            ClusterTask clusterTask = tektonClient.v1beta1().clusterTasks().withName(taskRef.getName()).get();
            params = clusterTask.getSpec().getParams();
        } else {
            Task task = tektonClient.v1beta1().tasks().inNamespace(component.getMetadata().getNamespace())
                    .withName(taskRef.getName()).get();
            params = task.getSpec().getParams();
        }

        if (params == null) {
            throw new MissingResourceException(
                    "{} '{}' implementation of Builder '{}' requested by Component '{}' is not found",
                    taskRef.getKind(), taskRef.getName(),
                    builderName, component.getFullName());
        }

        TaskRef buildTaskRef = new TaskRefBuilder()
                .withApiVersion("") // AGOGOS-96
                .withKind(taskRef.getKind())
                .withName(taskRef.getName())
                .build();

        // Prepare main task
        PipelineTaskBuilder pipelineTaskBuilder = new PipelineTaskBuilder()
                .withName(BUILD_PIPELINE_BUILDER_TASK_NAME)
                .withTaskRef(buildTaskRef)
                .withWorkspaces(workspaceBindings(builder.getSpec().getWorkspaces()));

        addParams(pipelineTaskBuilder, params, component.getSpec().getBuild().getParams());

        PipelineTask pipelineTask = pipelineTaskBuilder.build();

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

    @SuppressWarnings("unchecked")
    private void addParams(PipelineTaskBuilder pipelineTaskBuilder, List<io.fabric8.tekton.pipeline.v1beta1.ParamSpec> list,
            Map<String, Object> params) {
        list.forEach(p -> {
            Object value = params.get(p.getName());

            if (p.getName().equals("component")) {
                // Pass the component YAML, which came through as a Pipeline parameter.
                value = "$(params.component)";
            } else if (value == null) {
                return;
            }

            ParamValueBuilder pvb = new ParamValueBuilder();
            if (value instanceof List && isListOfStrings((List<Object>) value)) {
                pvb.withArrayVal((List<String>) value);
            } else if (value instanceof String) {
                pvb.withStringVal(value.toString());
            } else {
                pvb.withStringVal(objectMapper.asJson(value));
            }

            pipelineTaskBuilder.addNewParam().withName(p.getName()).withValue(pvb.build()).endParam();

        });
    }

    private boolean isListOfStrings(List<Object> array) {
        for (Object e : array) {
            if (!(e instanceof String)) {
                return false;
            }
        }

        return true;
    }
}
