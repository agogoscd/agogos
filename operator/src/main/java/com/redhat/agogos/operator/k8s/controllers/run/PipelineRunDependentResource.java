package com.redhat.agogos.operator.k8s.controllers.run;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.core.v1alpha1.WorkspaceMapping;
import com.redhat.agogos.operator.k8s.controllers.AbstractDependentResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PipelineRunDependentResource extends AbstractDependentResource<PipelineRun, Run> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunDependentResource.class);

    public PipelineRunDependentResource() {
        super(PipelineRun.class);
    }

    @ConfigProperty(name = "agogos.security.pod-security-policy.runAsNonRoot")
    Optional<Boolean> runAsNonRoot;

    @ConfigProperty(name = "agogos.security.pod-security-policy.runAsUser")
    Optional<Long> runAsUser;

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    @Override
    protected PipelineRun desired(Run resource, Context<Run> context) {
        PipelineRun pipelineRun = new PipelineRun();
        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (!optional.isEmpty()) {
            pipelineRun = optional.get();
            LOG.debug("{} '{}', using existing PipelineRun '{}'", resource.getKind(),
                    resource.getFullName(), fullPipelineRunName(pipelineRun));
        } else {
            LOG.debug("{} '{}', creating new PipelineRun", resource.getKind(), resource.getFullName());
        }

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("storage", Quantity.parse("1Gi")); // TODO: This needs to be configurable

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources().withRequests(requests).endResources()
                .withStorageClassName(storageClass.orElse(""))
                .endSpec()
                .build();

        WorkspaceBinding workspace = new WorkspaceBindingBuilder()
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withVolumeClaimTemplate(pvc)
                .build();

        Map<String, String> labels = new HashMap<>();
        labels.put(Label.RESOURCE.toString(), parentResource(resource).getKind().toLowerCase());
        labels.put(Label.NAME.toString(), parentResource(resource).getMetadata().getName());
        labels.put(Label.INSTANCE.toString(), resource.getMetadata().getLabels().get(Label.INSTANCE.toString()));

        PodSecurityContext podSecurityContext = new PodSecurityContextBuilder()
                .withRunAsNonRoot(runAsNonRoot.orElse(true))
                .withRunAsUser(runAsUser.orElse(65532l))
                .build();

        pipelineRun = new PipelineRunBuilder(pipelineRun)
                .withNewMetadata()
                .withLabels(labels)
                .withName(resource.getMetadata().getName()) // Name should match Run name.
                .withNamespace(resource.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewPipelineRef().withName(resource.getSpec().getPipeline()).endPipelineRef()
                .withWorkspaces(workspace)
                .withNewPodTemplate()
                .withSecurityContext(podSecurityContext)
                .endPodTemplate()
                .endSpec()
                .build();

        if (resource != null) {
            OwnerReference ownerReference = new OwnerReferenceBuilder()
                    .withApiVersion(resource.getApiVersion())
                    .withKind(resource.getKind())
                    .withName(resource.getMetadata().getName())
                    .withUid(resource.getMetadata().getUid())
                    .withBlockOwnerDeletion(true)
                    .withController(true)
                    .build();

            pipelineRun.getMetadata().setOwnerReferences(Arrays.asList(ownerReference));
        }

        if (serviceAccount.isPresent()) {
            pipelineRun.getSpec().setServiceAccountName(serviceAccount.get());
        }

        LOG.debug("PipelineRun '{}' created for '{}'", fullPipelineRunName(pipelineRun), resource.getFullName());
        LOG.error(objectMapper.asYaml(pipelineRun));
        return pipelineRun;
    }

    private String fullPipelineRunName(PipelineRun pipelineRun) {
        return String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                (pipelineRun.getMetadata().getName() != null ? pipelineRun.getMetadata().getName() : "<no-name-yet>"));
    }

    @Override
    protected Pipeline parentResource(Run run) {
        Pipeline pipeline = kubernetesFacade.get(
                Pipeline.class,
                run.getMetadata().getNamespace(),
                run.getSpec().getPipeline());

        if (pipeline == null) {
            throw new ApplicationException("Could not find Pipeline '{}' in namespace '{}'",
                    run.getSpec().getPipeline(), run.getMetadata().getNamespace());
        }

        return pipeline;
    }
}
