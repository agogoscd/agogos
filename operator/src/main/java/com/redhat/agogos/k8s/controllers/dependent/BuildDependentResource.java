package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Build;
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
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BuildDependentResource extends CRUDKubernetesDependentResource<PipelineRun, Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildDependentResource.class);

    @ConfigProperty(name = "agogos.security.pod-security-policy.runAsNonRoot")
    Optional<Boolean> runAsNonRoot;

    @ConfigProperty(name = "agogos.security.pod-security-policy.runAsUser")
    Optional<Long> runAsUser;

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    public BuildDependentResource() {
        super(PipelineRun.class);
    }

    @Override
    protected PipelineRun desired(Build build, Context<Build> context) {
        PipelineRun pipelinerun = new PipelineRun();
        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (!optional.isEmpty()) {
            LOG.debug("Build {}, using existing pipeline run", build.getFullName());
            pipelinerun = optional.get();
        } else {
            LOG.debug("Build {}, creating new pipeline run", build.getFullName());
        }

        AgogosResource<?, ?> parent = build.parentComponent();
        String name = parent.getMetadata().getName();
        String namespace = parent.getMetadata().getNamespace();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("storage", Quantity.parse("1Gi"));

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder() //
                .withNewSpec() //
                .withAccessModes("ReadWriteOnce") //
                .withNewResources().withRequests(requests).endResources() //
                .withStorageClassName(storageClass.orElse("")) //
                .endSpec() //
                .build();

        WorkspaceBinding workspace = new WorkspaceBindingBuilder() //
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME) //
                .withVolumeClaimTemplate(pvc) //
                .build();

        Map<String, String> labels = new HashMap<>();
        labels.put(Resource.RESOURCE.getLabel(), parent.getKind().toLowerCase());

        PodSecurityContext podSecurityContext = new PodSecurityContextBuilder() //
                .withRunAsNonRoot(runAsNonRoot.orElse(true)) //
                .withRunAsUser(runAsUser.orElse(65532l)) //
                .build();

        pipelinerun = new PipelineRunBuilder(pipelinerun) //
                .withNewMetadata() //
                .withLabels(labels) //
                .withGenerateName(name + "-") //
                .withNamespace(namespace) //
                .endMetadata() //
                .withNewSpec() //
                .withNewPipelineRef().withName(name).endPipelineRef() //
                .withWorkspaces(workspace) //
                .withNewPodTemplate() //
                .withSecurityContext(podSecurityContext) //
                .endPodTemplate() //
                .endSpec() //
                .build();

        if (build != null) {
            OwnerReference ownerReference = new OwnerReferenceBuilder() //
                    .withApiVersion(build.getApiVersion()) //
                    .withKind(build.getKind()) //
                    .withName(build.getMetadata().getName()) //
                    .withUid(build.getMetadata().getUid()) //
                    .withBlockOwnerDeletion(true) //
                    .withController(true) //
                    .build();

            pipelinerun.getMetadata().setOwnerReferences(Arrays.asList(ownerReference));
        }

        if (serviceAccount.isPresent()) {
            pipelinerun.getSpec().setServiceAccountName(serviceAccount.get());
        }

        return pipelinerun;
    }
}
