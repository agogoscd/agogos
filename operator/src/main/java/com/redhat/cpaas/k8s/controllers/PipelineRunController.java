package com.redhat.cpaas.k8s.controllers;

import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.v1alpha1.PipelineRunResource;
import com.redhat.cpaas.v1alpha1.PipelineRunResource.RunStatus;
import com.redhat.cpaas.v1alpha1.PipelineRunResource.Status;
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
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class PipelineRunController implements ResourceController<PipelineRunResource> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunController.class);

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    @Inject
    TektonClient tektonClient;

    @Inject
    TektonResourceClient tektonResourceClient;

    @Override
    public DeleteControl deleteResource(PipelineRunResource run, Context<PipelineRunResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<PipelineRunResource> createOrUpdateResource(PipelineRunResource run,
            Context<PipelineRunResource> context) {
        LOG.info("PipelineRun '{}' modified", run.getNamespacedName());

        try {
            switch (Status.valueOf(run.getStatus().getStatus())) {
                case New:
                    LOG.info("Handling new pipeline run '{}'", run.getNamespacedName());

                    // TODO: Externalize it!
                    // TODO: This code can be made generic to work for any pipelines
                    Pipeline pipeline = tektonResourceClient.getPipelineByName(run.getSpec().getPipeline());

                    if (pipeline == null) {
                        setStatus(run, Status.Failed, "Pipeline not found");
                        return UpdateControl.updateStatusSubResource(run);
                    }

                    PipelineRef pipelineRef = new PipelineRefBuilder(true).withName(pipeline.getMetadata().getName())
                            .build();

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

                    OwnerReference ownerReference = new OwnerReferenceBuilder() //
                            .withApiVersion(run.getApiVersion()) //
                            .withKind(run.getKind()) //
                            .withName(run.getMetadata().getName()) //
                            .withUid(run.getMetadata().getUid()) //
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
                            .withName(run.getMetadata().getName()) //
                            // .withLabels(labels) //
                            .endMetadata() //
                            .withSpec(pipelineRunSpecBuilder.build()) //
                            .build();

                    tektonClient.v1beta1().pipelineRuns().inNamespace(run.getMetadata().getNamespace())
                            .create(pipelineRun);

                    // Set build status to "Running"
                    setStatus(run, Status.Running, "Pipeline triggered");
                    return UpdateControl.updateStatusSubResource(run);
                default:
                    break;
            }
        } catch (Exception ex) {
            LOG.error("An error occurred while handling PipelineRun object '{}' modification",
                    run.getNamespacedName(), ex);

            // Set build status to "Failed"
            setStatus(run, Status.Failed, ex.getMessage());

            return UpdateControl.updateStatusSubResource(run);
        }

        return UpdateControl.noUpdate();
    }

    // TODO: Make this shared across controllers
    private boolean setStatus(PipelineRunResource run, Status status, String reason) {
        RunStatus runStatus = run.getStatus();

        if (runStatus.getStatus().equals(String.valueOf(status)) && runStatus.getReason().equals(reason)) {
            return false;
        }

        runStatus.setStatus(String.valueOf(status));
        runStatus.setReason(reason);
        runStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }
}
