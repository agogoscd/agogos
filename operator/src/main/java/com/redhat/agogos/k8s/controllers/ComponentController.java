package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentHandlerSpec;
import com.redhat.agogos.v1alpha1.Handler;
import com.redhat.agogos.v1alpha1.Status;
import com.redhat.agogos.v1alpha1.WorkspaceMapping;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpec;
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
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@ControllerConfiguration
public class ComponentController implements Reconciler<Component>, Cleaner<Component> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentController.class);

    private static final String BUILD_PIPELINE_INIT_TASK_NAME = "init";
    private static final String BUILD_PIPELINE_ARTIFACT_TASK_NAME_PREFIX = "artifact-";
    private static final String BUILD_PIPELINE_SOURCE_TASK_NAME = "fetch-source";
    private static final String BUILD_PIPELINE_BUILDER_TASK_NAME = "build";

    private static final String BUILD_PIPELINE_DEFAULT_TASK_WORKSPACE_NAME = "output";

    private static final String BUILD_PIPELINE_INIT_TASK_WORKSPACE_NAME = "output";
    private static final String BUILD_PIPELINE_SOURCE_TASK_WORKSPACE_NAME = "output";
    private static final String BUILD_PIPELINE_BUILDER_TASK_WORKSPACE_NAME = "output";

    @Inject
    AgogosClient agogosClient;

    @Inject
    TektonClient tektonClient;

    @Inject
    ObjectMapper objectMapper;

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
    public DeleteControl cleanup(Component component, Context<Component> context) {
        LOG.info("Removing component '{}'", component.getFullName());
        return DeleteControl.defaultDelete();
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
    public UpdateControl<Component> reconcile(Component component, Context<Component> context) {
        return onResourceUpdate(component, context);
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
            LOG.debug("Preparing pipeline for Component '{}'", component.getFullName());

            this.updateTektonBuildPipeline(component);

            setStatus(component, ResourceStatus.Ready, "Component is ready");

            LOG.info("Pipeline for Component '{}' updated", component.getFullName());
        } catch (ApplicationException e) {
            LOG.error("Error occurred while creating pipeline for component '{}'", component.getFullName(), e);

            setStatus(component, ResourceStatus.Failed, "Could not create Component: " + e.getMessage());
        }

        return UpdateControl.updateStatus(component);
    }

    /**
     * <p>
     * Prepares the 'init' task which is responsible for fetching information about the resource that should be built.
     * </p>
     */
    private PipelineTask prepareInitTask(List<PipelineTask> tasks, Component component) {
        ClusterTask initTask = tektonClient.v1beta1().clusterTasks().withName(BUILD_PIPELINE_INIT_TASK_NAME).get();

        if (initTask == null) {
            throw new MissingResourceException("Could not find 'init' ClusterTask in the system");
        }

        // Construct the resource in format: [PLURAL].[VERSION].[GROUP]/[NAME]
        // For example: 'components.v1alpha1.agogos.redhat.com/test'
        String resource = new StringBuilder() //
                .append(component.getPlural()) //
                .append(".") //
                .append(component.getVersion()) //
                .append(".") //
                .append(component.getGroup()) //
                .append("/") //
                .append(component.getMetadata().getName()) //
                .toString();

        WorkspacePipelineTaskBinding workspaceBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName(BUILD_PIPELINE_INIT_TASK_WORKSPACE_NAME) //
                .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME) //
                .build();

        // Build the init task
        PipelineTask pipelineTask = new PipelineTaskBuilder() //
                .withName(BUILD_PIPELINE_INIT_TASK_NAME) //
                .withTaskRef(new TaskRefBuilder() //
                        .withName(initTask.getMetadata().getName())
                        .withApiVersion(initTask.getApiVersion()) //
                        .withKind(initTask.getKind()) //
                        .build()) //
                .addNewParam() //
                .withName("resource") //
                .withNewValue(resource) //
                .endParam() //
                .withWorkspaces(workspaceBinding) //
                .build();

        tasks.add(pipelineTask);

        return pipelineTask;
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
            WorkspacePipelineTaskBinding defaultTaskBinding = new WorkspacePipelineTaskBindingBuilder() //
                    .withName(BUILD_PIPELINE_DEFAULT_TASK_WORKSPACE_NAME)
                    .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME) //
                    .build();

            workspaceBindings.add(defaultTaskBinding);
        } else {
            mappings.forEach(workspaceMapping -> {
                String targetWorkspaceName = BUILD_PIPELINE_SOURCE_TASK_WORKSPACE_NAME;

                // Handle workspace mappings, if provided
                if (workspaceMapping.getName() != null) {
                    targetWorkspaceName = workspaceMapping.getName();
                }

                WorkspacePipelineTaskBinding pipelineTaskBinding = new WorkspacePipelineTaskBindingBuilder() //
                        .withName(targetWorkspaceName)
                        .withWorkspace(WorkspaceMapping.MAIN_WORKSPACE_NAME) //
                        .withSubPath(workspaceMapping.getSubPath()) //
                        .build();

                workspaceBindings.add(pipelineTaskBinding);
            });
        }

        return workspaceBindings;
    }

    private void prepareHandlerTasks(Component component, List<ComponentHandlerSpec> handlers, List<PipelineTask> tasks) {
        handlers.stream().forEach(handlerSpec -> {
            String handlerName = handlerSpec.getHandlerRef().getName();

            Handler handler = agogosClient.v1alpha1().handlers().inNamespace(component.getMetadata().getNamespace())
                    .withName(handlerName).get();

            Task handlerTask = tektonClient.v1beta1().tasks().inNamespace(component.getMetadata().getNamespace())
                    .withName(handler.getSpec().getTaskRef().getName()).get();

            PipelineTask lastTask = tasks.get(tasks.size() - 1);

            PipelineTaskBuilder pipelineTaskBuilder = new PipelineTaskBuilder() //
                    .withName(handlerName) //
                    .withRunAfter(lastTask.getName()) //
                    .withTaskRef(
                            new TaskRefBuilder().withName(handlerTask.getMetadata().getName())
                                    .withApiVersion(handlerTask.getApiVersion())
                                    .withKind(handlerTask.getKind())
                                    .build()) //
                    .withWorkspaces(
                            workspaceBindings(handler.getSpec().getWorkspaces()));

            addParams(pipelineTaskBuilder, handlerTask.getSpec().getParams(), handlerSpec.getParams());

            PipelineTask pipelineTask = pipelineTaskBuilder.build();

            tasks.add(pipelineTask);
        });
    }

    private boolean isListOfStrings(List<Object> array) {
        boolean strings = true;

        for (Object e : array) {
            if (!(e instanceof String)) {
                strings = false;
                break;
            }
        }

        return strings;
    }

    private PipelineTask prepareBuilderTask(Component component, List<PipelineTask> tasks) {
        String builderName = component.getSpec().getBuild().getBuilderRef().getName();

        Builder builder = agogosClient.v1alpha1().builders().withName(builderName)
                .get();

        if (builder == null) {
            throw new MissingResourceException("Selected Builder '{}' is not available in the system",
                    builderName);
        }

        // TODO: ClusterTask support?
        Task builderTask = tektonClient.v1beta1().tasks().inNamespace(component.getMetadata().getNamespace())
                .withName(builder.getSpec().getTaskRef().getName()).get();

        if (builderTask == null) {
            throw new MissingResourceException(
                    "Task '{}' being implementation of Builder '{}' requested by '{}' Component is not found",
                    builder.getSpec().getTaskRef().getName(),
                    builderName, component.getFullName());
        }

        PipelineTask lastTask = tasks.get(tasks.size() - 1);

        TaskRef buildTaskRef = new TaskRefBuilder() //
                .withApiVersion(HasMetadata.getApiVersion(Task.class)) //
                .withKind(builder.getSpec().getTaskRef().getKind()) //
                .withName(builder.getSpec().getTaskRef().getName()) //
                .build();

        // Prepare main task
        PipelineTaskBuilder pipelineTaskBuilder = new PipelineTaskBuilder() //
                .withName(BUILD_PIPELINE_BUILDER_TASK_NAME) //
                .withTaskRef(buildTaskRef)
                .withWorkspaces(
                        workspaceBindings(builder.getSpec().getWorkspaces())) //
                .withRunAfter(lastTask.getName());

        addParams(pipelineTaskBuilder, builderTask.getSpec().getParams(), component.getSpec().getBuild().getParams());

        PipelineTask pipelineTask = pipelineTaskBuilder.build();

        tasks.add(pipelineTask);

        return pipelineTask;
    }

    private void addParams(PipelineTaskBuilder pipelineTaskBuilder, List<ParamSpec> taskParams, Map<String, Object> params) {
        taskParams.forEach(p -> {
            Object value = params.get(p.getName());

            if (value == null) {
                return;
            }

            ArrayOrString converted = null;

            if (value instanceof List && isListOfStrings((List<Object>) value)) {
                converted = new ArrayOrString((List) value);
            } else if (value instanceof String) {
                converted = new ArrayOrString(value.toString());
            } else {
                try {
                    converted = new ArrayOrString(
                            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
                } catch (JsonProcessingException e) {
                    throw new ApplicationException("Could not convert '{}' parameter to JSON", p.getName(), e);
                }
            }

            pipelineTaskBuilder.addNewParam().withName(p.getName()).withValue(converted).endParam();

        });
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
        List<PipelineTask> tasks = new ArrayList<>();

        // Add the init task
        prepareInitTask(tasks, component);

        // Add any pre Handlers
        prepareHandlerTasks(component, component.getSpec().getPre(), tasks);

        // Finally add the Builder task
        PipelineTask buildTask = prepareBuilderTask(component, tasks);

        // Add any post Handlers
        prepareHandlerTasks(component, component.getSpec().getPost(), tasks);

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME) //
                .withDescription("Main workspace that is shared across each task in the build pipeline") //
                .build();

        // Pipeline result is the result of the main task executed
        PipelineResult pipelineResult = new PipelineResultBuilder() //
                .withName("data") //
                .withValue(new StringBuilder().append("$(tasks.").append(buildTask.getName())
                        .append(".results.data)").toString()) //
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
                //.withResults(pipelineResult) //
                .endSpec() //
                .build();

        return tektonClient.v1beta1().pipelines().inNamespace(component.getMetadata().getNamespace())
                .createOrReplace(pipeline);
    }

}
