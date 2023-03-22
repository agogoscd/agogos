package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.ResultableStatus;
import com.redhat.agogos.v1alpha1.WorkspaceMapping;
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

public abstract class AbstractPipelineRunDependentResource<T extends AgogosResource<?, ResultableStatus>>
        extends AbstractBaseDependentResource<PipelineRun, T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPipelineRunDependentResource.class);

    @ConfigProperty(name = "agogos.security.pod-security-policy.runAsNonRoot")
    Optional<Boolean> runAsNonRoot;

    @ConfigProperty(name = "agogos.security.pod-security-policy.runAsUser")
    Optional<Long> runAsUser;

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    public AbstractPipelineRunDependentResource() {
        super(PipelineRun.class);
    }

    public abstract String resourceName(T resource);

    @Override
    protected PipelineRun desired(T resource, Context<T> context) {
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
        labels.put(Resource.RESOURCE.getLabel(), parentResource(resource).getKind().toLowerCase());

        PodSecurityContext podSecurityContext = new PodSecurityContextBuilder()
                .withRunAsNonRoot(runAsNonRoot.orElse(true))
                .withRunAsUser(runAsUser.orElse(65532l))
                .build();

        pipelineRun = new PipelineRunBuilder(pipelineRun)
                .withNewMetadata()
                .withLabels(labels)
                .withGenerateName(resourceName(resource) + "-")
                .withName(pipelineRun.getMetadata() != null ? pipelineRun.getMetadata().getName() : null)
                .withNamespace(resource.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewPipelineRef().withName(resourceName(resource)).endPipelineRef()
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
        return pipelineRun;
    }

    private String fullPipelineRunName(PipelineRun pipelineRun) {
        return String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                (pipelineRun.getMetadata().getName() != null ? pipelineRun.getMetadata().getName() : "<no-name-yet>"));
    }
}
