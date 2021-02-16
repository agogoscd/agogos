package com.redhat.cpaas.k8s.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.k8s.client.ComponentBuildResourceClient;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.k8s.event.PipelineRunEvent;
import com.redhat.cpaas.k8s.event.PipelineRunEventSource;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource.BuildStatus;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource.Status;
import com.redhat.cpaas.v1alpha1.ComponentResource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRef;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunResult;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunStatus;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;

@Controller(generationAwareEventProcessing = false)
public class ComponentBuildController implements ResourceController<ComponentBuildResource> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentBuildController.class);

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    @Inject
    ComponentResourceClient componentResourceClient;

    @Inject
    ComponentBuildResourceClient componentBuildResourceClient;

    @Inject
    TektonResourceClient tektonResourceClient;

    @Inject
    PipelineRunEventSource pipelineRunEventSource;

    @Inject
    TektonClient tektonClient;

    /**
     * <p>
     * Register the {@link io.fabric8.tekton.pipeline.v1alpha1.PipelineRun} event
     * source so that we can receive events from PipelineRun's that are related to
     * {@link ComponentResource}'s.
     * </p>
     */
    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("pipeline-run", pipelineRunEventSource);
    }

    /**
     * <p>
     * Method triggered when a {@link ComponentBuildResource} is removed from the
     * cluster.
     * </p>
     * 
     * @param component {@link ComponentBuildResource}
     * @param context   {@link Context}
     * @return {@link DeleteControl}
     */
    @Override
    public DeleteControl deleteResource(ComponentBuildResource build, Context<ComponentBuildResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    /**
     * <p>
     * Triggers or updates the Tekton Pipeline based on the
     * {@link ComponentBuildResource} data passed and sets the status subresource on
     * {@link ComponentBuildResource} depending on the outcome of the pipeline
     * update.
     * </p>
     * 
     * @param componentBuild {@link ComponentBuildResource}
     */
    @Override
    public UpdateControl<ComponentBuildResource> createOrUpdateResource(ComponentBuildResource componentBuild,
            Context<ComponentBuildResource> context) {

        Optional<CustomResourceEvent> crEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);

        if (crEvent.isPresent()) {
            LOG.info("Build '{}' modified", componentBuild.getNamespacedName());

            try {
                if (Status.valueOf(componentBuild.getStatus().getStatus()) == Status.New) {
                    LOG.info("Handling new build '{}'", componentBuild.getNamespacedName());

                    // Run pipeline
                    runBuildPipeline(componentBuild);

                    // Set build status to "Running"
                    setStatus(componentBuild, Status.Running, "Pipeline triggered");
                    return UpdateControl.updateStatusSubResource(componentBuild);
                }
            } catch (Exception ex) {
                LOG.error("An error occurred while handling build object '{}' modification",
                        componentBuild.getNamespacedName(), ex);

                // Set build status to "Failed"
                setStatus(componentBuild, Status.Failed, ex.getMessage());

                return UpdateControl.updateStatusSubResource(componentBuild);
            }
        }

        Optional<PipelineRunEvent> pipelineRunEvent = context.getEvents().getLatestOfType(PipelineRunEvent.class);

        if (pipelineRunEvent.isPresent()) {
            return handlePipelineRunEvent(componentBuild, pipelineRunEvent.get());
        }

        return UpdateControl.noUpdate();
    }

    /**
     * <p>
     * Updates {@link ComponentBuildResource.BuildStatus} of the particular
     * {@link ComponentBuildResource}.
     * <p/>
     * 
     * <p>
     * It sets result of the status to <code>null</code>.
     * </p>
     * 
     * 
     * @param component {@link ComponentBuildResource} object
     * @param status    One of available statuses
     * @param reason    Description of the reason for last status change
     */
    private boolean setStatus(ComponentBuildResource build, Status status, String reason) {
        return setStatus(build, status, reason, null);
    }

    /**
     * <p>
     * Updates {@link ComponentBuildResource.BuildStatus} of the particular
     * {@link ComponentBuildResource}.
     * <p/>
     * 
     * 
     * @param component {@link ComponentBuildResource} object
     * @param status    One of available statuses
     * @param reason    Description of the reason for last status change
     * @param result    String formatted result, if any
     */
    private boolean setStatus(ComponentBuildResource build, Status status, String reason, String result) {
        BuildStatus buildStatus = build.getStatus();

        if (buildStatus.getStatus().equals(String.valueOf(status)) && buildStatus.getReason().equals(reason)
                && buildStatus.getResult() != null && buildStatus.getResult().equals(result)) {
            return false;
        }

        buildStatus.setStatus(String.valueOf(status));
        buildStatus.setReason(reason);
        buildStatus.setResult(result);
        buildStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }

    /**
     * <p>
     * Triggers Tekton pipeline of that is linked to the {@link ComponentResource}
     * the {@link ComponentBuildResource} is pointing to.
     * </p>
     * 
     * @param componentBuild The {@link ComponentBuildResource}
     * @throws ApplicationException in case the pipeline cannot be triggered
     */
    private void runBuildPipeline(ComponentBuildResource componentBuild) throws ApplicationException {
        LOG.info("Triggering pipeline for build '{}'", componentBuild.getNamespacedName());

        ComponentResource component = componentResourceClient.getByName(componentBuild.getSpec().getComponent());

        if (component == null) {
            throw new ApplicationException("Could not find component with name '{}' in namespace '{}'",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        if (!component.isReady()) {
            throw new ApplicationException("Component '{}'' is not ready to build it", component.getNamespacedName());
        }

        try {
            this.tektonRunBuildPipeline(componentBuild.getSpec().getComponent(),
                    componentBuild.getMetadata().getName());
        } catch (ApplicationException e) {
            throw new ApplicationException("Error occurred while triggering the pipeline for component '{}'",
                    component.getNamespacedName(), e);
        }
    }

    /**
     * <p>
     * Updates the status of particular {@link ComponentBuildResource} based on the
     * event received from a {@link io.fabric8.tekton.pipeline.v1beta1.PipelineRun}.
     * </p>
     * 
     * @param build the build to
     * @param event
     * @return
     */
    public UpdateControl<ComponentBuildResource> handlePipelineRunEvent(ComponentBuildResource build,
            PipelineRunEvent event) {

        PipelineRunStatus pipelineRunStatus = event.getStatus();

        if (pipelineRunStatus == null) {
            return UpdateControl.noUpdate();
        }

        // Get latest condition
        Condition condition = pipelineRunStatus.getConditions().get(0);

        Status status = null;
        String message = null;
        String result = null;

        boolean update = false;

        switch (condition.getStatus()) {
            case "Unknown":
                status = Status.Running;
                message = "Build in progress";
                break;
            case "True":
                status = Status.Passed;
                message = "Build finished";

                List<PipelineRunResult> pipelineResults = pipelineRunStatus.getPipelineResults();

                if (!pipelineResults.isEmpty()) {
                    result = pipelineResults.get(0).getValue();
                }

                // TODO: For successful run we should cleanup Tekton's PipelineRun

                if (!condition.getReason().equals("Succeeded")) {
                    message = "Build finished but some tasks were skipped";
                }
                break;
            case "False":
                if (condition.getReason().equals("PipelineRunCancelled")) {
                    status = Status.Aborted;
                    message = "Build aborted";
                } else {
                    status = Status.Failed;
                    message = "Build failed";
                }

                break;
        }

        // Update status in the object and check whether it was necessary
        update = setStatus(build, status, message, result);

        if (update) {
            LOG.debug("Updating build '{}' with PipelineRun status '{}'", build.getNamespacedName(),
                    condition.getReason());
            return UpdateControl.updateStatusSubResource(build);
        }

        return UpdateControl.noUpdate();

    }

    private PipelineRun tektonRunBuildPipeline(String componentName, String buildName) throws ApplicationException {
        Pipeline pipeline = tektonResourceClient.getPipelineByName(componentName);

        if (pipeline == null) {
            throw new MissingResourceException("Pipeline '{}' not found in the system", componentName);
        }

        PipelineRef pipelineRef = new PipelineRefBuilder(true).withName(pipeline.getMetadata().getName()).build();

        Map<String, Quantity> requests = new HashMap<String, Quantity>();
        requests.put("storage", new Quantity("1Gi"));

        String storageClassName = "";

        if (storageClass.isPresent()) {
            storageClassName = storageClass.get();
        }

        String serviceAccountName = null;

        if (serviceAccount.isPresent()) {
            serviceAccountName = serviceAccount.get();
        }

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder() //
                .withNewSpec() //
                .withNewResources().withRequests(requests).endResources() //
                .withStorageClassName(storageClassName) //
                .withAccessModes("ReadWriteOnce") //
                .endSpec()//
                .build();

        WorkspaceBinding workspaceBinding = new WorkspaceBindingBuilder() //
                .withName("ws") //
                .withVolumeClaimTemplate(pvc) //
                .build();

        ComponentBuildResource build = componentBuildResourceClient.getByName(buildName);

        if (build == null) {
            throw new MissingResourceException("Selected build '{}' does not exist", buildName);
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("cpaas.redhat.com/build", buildName);

        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(build.getApiVersion()) //
                .withKind(build.getKind()) //
                .withName(build.getMetadata().getName()) //
                .withUid(build.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        PipelineRunSpecBuilder pipelineRunSpecBuilder = new PipelineRunSpecBuilder() //
                .withPipelineRef(pipelineRef) //
                .withWorkspaces(workspaceBinding); //

        if (serviceAccountName != null) {
            pipelineRunSpecBuilder.withServiceAccountName(serviceAccountName);
        }

        PipelineRun pipelineRun = new PipelineRunBuilder() //
                .withNewMetadata() //
                .withOwnerReferences(ownerReference) //
                .withName(buildName) //
                .withLabels(labels) //
                .endMetadata() //
                .withSpec(pipelineRunSpecBuilder.build()) //
                .build();

        return tektonClient.v1beta1().pipelineRuns().inNamespace(build.getMetadata().getNamespace())
                .create(pipelineRun);
    }
}
