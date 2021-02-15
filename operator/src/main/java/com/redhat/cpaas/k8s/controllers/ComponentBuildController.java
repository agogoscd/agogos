package com.redhat.cpaas.k8s.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRef;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(generationAwareEventProcessing = false)
public class ComponentBuildController implements ResourceController<ComponentBuildResource> {

    private static final Logger LOG = LoggerFactory.getLogger( ComponentBuildController.class);

    private static final String COMPONENT_LABEL = "cpaas.redhat.com/component";

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ComponentBuildResourceClient buildResourceClient;

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

    private boolean hasLabels(ComponentBuildResource build) {
        Map<String, String> labels = build.getMetadata().getLabels();

        if (labels == null) {
            return false;
        }

        if (labels.get(COMPONENT_LABEL) != null) {
            return true;
        }

        return false;
    }

    private boolean setStatus(ComponentBuildResource build, Status status, String reason) {
        BuildStatus buildStatus = build.getStatus();

        if (buildStatus.getStatus().equals(String.valueOf(status)) && buildStatus.getReason().equals(reason)) {
            return false;
        }

        buildStatus.setStatus(String.valueOf(status));
        buildStatus.setReason(reason);
        buildStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }

    // TODO: Use proper exceptions!
    private void runPipeline(ComponentBuildResource build) throws Exception {
        LOG.info("New build, triggering pipeline for this build");

        ComponentResource component = componentResourceClient.getByName(build.getSpec().getComponent());

        if (component == null) {
            throw new Exception("Could not find component with name '{}'" + build.getSpec().getComponent() + "'");
        }

        if (!component.isReady()) {
            throw new Exception("Component is not ready to build it");
        }

        try {
            this.runBuildPipeline(build.getSpec().getComponent(), build.getMetadata().getName());
        } catch (ApplicationException e) {
            throw new Exception("Error occurred while triggering the pipeline for component '{}'", e);
        }
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("pipeline-run", pipelineRunEventSource);
    }

    @Override
    public DeleteControl deleteResource(ComponentBuildResource build, Context<ComponentBuildResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    public UpdateControl<ComponentBuildResource> onResourceUpdate(ComponentBuildResource build,
            Context<ComponentBuildResource> context) {

        LOG.info("Build '{}' modified", build.getMetadata().getName());

        try {
            switch (Status.valueOf(build.getStatus().getStatus())) {
                case New:
                    LOG.info("Handling new build '{}'", build.getMetadata().getName());

                    if (hasLabels(build)) {
                        // Run pipeline
                        runPipeline(build);

                        // Set build status to "Running"
                        setStatus(build, Status.Running, "Pipeline triggered");
                        return UpdateControl.updateStatusSubResource(build);
                    } else {
                        // Prepare labels
                        Map<String, String> labels = new HashMap<>();
                        labels.put(COMPONENT_LABEL, build.getSpec().getComponent());

                        // Set labels
                        build.getMetadata().setLabels(labels);

                        // Update build
                        return UpdateControl.updateCustomResource(build);
                    }
                case Running:
                    // Check back later to see what is the status
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            LOG.error("An error occurred while handling build object '{}' modification",
                    build.getMetadata().getName(), ex);

            // Set build status to "Failed"
            setStatus(build, Status.Failed, ex.getMessage());

            return UpdateControl.updateStatusSubResource(build);
        }

        return UpdateControl.noUpdate();
    }

    /**
     * Updates the status of particular {@link ComponentBuildResource} based on the
     * event received from a {@link io.fabric8.tekton.pipeline.v1beta1.PipelineRun}.
     * 
     * @param build the build to
     * @param event
     * @return
     */
    public UpdateControl<ComponentBuildResource> updateStatus(ComponentBuildResource build, PipelineRunEvent event) {

        PipelineRunStatus status = event.getStatus();

        if (status == null) {
            return UpdateControl.noUpdate();
        }

        // Get latest condition
        Condition condition = status.getConditions().get(0);

        boolean update = false;

        switch (condition.getStatus()) {
            case "Unknown":
                update = setStatus(build, Status.Running, "Build in progress");
                break;
            case "True":
                if (condition.getReason().equals("Succeeded")) {
                    update = setStatus(build, Status.Passed, "Build finished");
                } else {
                    update = setStatus(build, Status.Passed, "Build finished but some tasks were skipped");
                }
                break;
            case "False":
                if (condition.getReason().equals("PipelineRunCancelled")) {
                    update = setStatus(build, Status.Aborted, "Build aborted");
                } else {
                    update = setStatus(build, Status.Failed, "Build failed");
                }

                break;
        }

        if (update) {
            LOG.debug("Updating build '{}' with PipelineRun status '{}'", build.getMetadata().getName(),
                    condition.getReason());
            return UpdateControl.updateStatusSubResource(build);
        }

        return UpdateControl.noUpdate();

    }

    public UpdateControl<ComponentBuildResource> onEvent(ComponentBuildResource resource,
            Context<ComponentBuildResource> context) {
        LOG.info(resource.toString());
        return UpdateControl.noUpdate();
    }

    @Override
    public UpdateControl<ComponentBuildResource> createOrUpdateResource(ComponentBuildResource resource,
            Context<ComponentBuildResource> context) {

        Optional<PipelineRunEvent> event = context.getEvents().getLatestOfType(PipelineRunEvent.class);

        if (event.isPresent()) {
            return updateStatus(resource, event.get());
        }

        return onResourceUpdate(resource, context);
        // final var customResourceEvent =
        // context.getEvents().getLatestOfType(PipelineRunEvent.class);
        // if (customResourceEvent.isPresent()) {
        // return onResourceUpdate(resource, context);
        // }
        // return onEvent(resource, context);
    }

    private PipelineRun runBuildPipeline(String componentName, String buildName) throws ApplicationException {
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
