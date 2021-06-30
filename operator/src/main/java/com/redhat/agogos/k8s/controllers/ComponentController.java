package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.SourceHandler;
import com.redhat.agogos.v1alpha1.Status;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.client.TektonClient;
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
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
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
import java.util.Optional;

@ApplicationScoped
@Controller
public class ComponentController implements ResourceController<Component> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentController.class);

    @Inject
    AgogosClient agogosClient;

    @Inject
    TektonClient tektonClient;

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
    public DeleteControl deleteResource(Component component, Context<Component> context) {
        LOG.info("Removing component '{}'", component.getFullName());
        return DeleteControl.DEFAULT_DELETE;
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
    public UpdateControl<Component> createOrUpdateResource(Component component,
            Context<Component> context) {

        // Try to find the latest event
        final Optional<CustomResourceEvent> customResourceEvent = context.getEvents()
                .getLatestOfType(CustomResourceEvent.class);

        // Handle it if available
        if (customResourceEvent.isPresent()) {
            return onResourceUpdate(component, context);
        }

        return UpdateControl.noUpdate();
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

        return UpdateControl.updateStatusSubResource(component);
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
        Builder builder = agogosClient.v1alpha1().builders().withName(component.getSpec().getBuilderRef().get("name")).get();

        if (builder == null) {
            throw new MissingResourceException("Selected Builder '{}' is not available in the system",
                    component.getSpec().getBuilderRef().get("name"));
        }

        SourceHandler sourceHandler = null;

        // In case the source handler is specified in the Component, validate if it exists in the system
        if (component.getSpec().getSource().getHandlerRef().getName() != null) {
            sourceHandler = agogosClient.v1alpha1().sourcehandlers()
                    .withName(component.getSpec().getSource().getHandlerRef().getName()).get();

            if (sourceHandler == null) {
                throw new MissingResourceException("Selected SourceHandler '{}' is not available in the system",
                        component.getSpec().getSource().getHandlerRef().getName());
            }
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

        String resource = new StringBuilder().append(component.getPlural()).append(".").append(component.getVersion())
                .append(".")
                .append(component.getGroup()).append("/").append(component.getMetadata().getName()).toString();

        PipelineTask initTask = new PipelineTaskBuilder() //
                .withName("init") //
                .withTaskRef(new TaskRefBuilder().withName("init").withApiVersion("tekton.dev/v1beta1").withKind("ClusterTask")
                        .build()) //
                .addNewParam() //
                .withName("resource") //
                .withNewValue(resource) //
                .endParam() //
                .withWorkspaces(pipelineWsBinding) //
                .build();

        tasks.add(initTask);

        String runBuildAfter = "init";

        if (sourceHandler != null) {
            Task sourceTask = tektonClient.v1beta1().tasks().inNamespace(component.getMetadata().getNamespace())
                    .withName(sourceHandler.getSpec().getTaskRef().getName()).get();

            // Prepare workspace for main task to share content between steps
            WorkspacePipelineTaskBinding sourceWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                    .withName("output") // TODO: This is specific for the git-clone task: https://hub.tekton.dev/tekton/task/git-clone
                    .withWorkspace("ws") //
                    .withSubPath("pipeline/source") //
                    .build();

            PipelineTaskBuilder sourceTaskBuilder = new PipelineTaskBuilder() //
                    .withName("source") //
                    .withRunAfter("init") //
                    .withTaskRef(
                            // TODO: unhardcode apiversion
                            new TaskRefBuilder().withName(sourceHandler.getSpec().getTaskRef().getName())
                                    .withApiVersion("tekton.dev/v1beta1")
                                    .withKind(sourceHandler.getSpec().getTaskRef().getKind())
                                    .build()) //
                    .withWorkspaces(sourceWsBinding);

            System.out.println(component.getSpec().getSource().getData());

            sourceTask.getSpec().getParams().forEach(p -> {
                //System.out.println(p);
                Object value = component.getSpec().getSource().getData().get(p.getName());

                System.out.println(value);

                if (value != null) {
                    sourceTaskBuilder.addNewParam().withName(p.getName()).withNewValue(value.toString()).endParam();
                }
            });

            tasks.add(sourceTaskBuilder.build());

            runBuildAfter = "source";
        }

        TaskRef buildTaskRef = new TaskRefBuilder().withApiVersion(HasMetadata.getApiVersion(Task.class))
                .withKind(builder.getSpec().getTaskRef().getKind()).withName(builder.getSpec().getTaskRef().getName()).build();

        // Prepare main task
        PipelineTask buildTask = new PipelineTaskBuilder() //
                .withName("builder") //
                .withTaskRef(buildTaskRef)
                .withWorkspaces(stageWsBinding, pipelineWsBinding) //
                .withRunAfter(runBuildAfter)
                .build();

        tasks.add(buildTask);

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName("ws") //
                .withDescription("Main workspace") //
                .build();

        // Pipeline result is the result of the main task executed
        PipelineResult pipelineResult = new PipelineResultBuilder() //
                .withName("data") //
                .withValue("$(tasks.builder.results.data)") //
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
                .withResults(pipelineResult) //
                .endSpec() //
                .build();

        return tektonClient.v1beta1().pipelines().inNamespace(component.getMetadata().getNamespace())
                .createOrReplace(pipeline);
    }

}
