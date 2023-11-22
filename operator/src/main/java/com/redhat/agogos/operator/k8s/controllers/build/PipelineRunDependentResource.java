package com.redhat.agogos.operator.k8s.controllers.build;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.WorkspaceMapping;
import com.redhat.agogos.operator.k8s.controllers.AbstractDependentResource;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.tekton.pipeline.v1beta1.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PipelineRunDependentResource extends AbstractDependentResource<PipelineRun, Build> {

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
    protected PipelineRun desired(Build build, Context<Build> context) {
        PipelineRun pipelineRun = new PipelineRun();
        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (!optional.isEmpty()) {
            pipelineRun = optional.get();
            LOG.debug("{} '{}', using existing PipelineRun '{}'", build.getKind(),
                    build.getFullName(), fullPipelineRunName(pipelineRun));
        } else {
            LOG.debug("{} '{}', creating new PipelineRun", build.getKind(), build.getFullName());
        }

        Component component = context.getSecondaryResource(Component.class).get();

        return createPipelineRun(build, component, pipelineRun);
    }

    private String fullPipelineRunName(PipelineRun pipelineRun) {
        return String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                (pipelineRun.getMetadata().getName() != null ? pipelineRun.getMetadata().getName() : "<no-name-yet>"));
    }

    protected Component parentResource(Build build) {
        LOG.debug("Finding parent Component for Build '{}'", build.getFullName());

        Component component = kubernetesFacade.get(Component.class, build.getMetadata().getNamespace(),
                build.getSpec().getComponent());
        if (component == null) {
            throw new ApplicationException("Could not find Component '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        return component;
    }

    public PipelineRun createPipelineRun(Build build, Component component, PipelineRun pipelineRun) {
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

        // FIXME: is this parentResource the same that is retrieved as secondaryResource?
        Component buildParent = parentResource(build);
        Map<String, String> labels = new HashMap<>();
        labels.put(Label.RESOURCE.toString(), buildParent.getKind().toLowerCase());
        labels.put(Label.NAME.toString(), buildParent.getMetadata().getName());
        labels.put(Label.INSTANCE.toString(), build.getMetadata().getLabels().get(Label.INSTANCE.toString()));

        PodSecurityContext podSecurityContext = new PodSecurityContextBuilder()
                .withRunAsNonRoot(runAsNonRoot.orElse(true))
                .withRunAsUser(runAsUser.orElse(65532l))
                .build();

        Param componentParam = new ParamBuilder()
                .withName("component")
                .withNewValue(objectMapper.asYaml(component))
                .build();

        Param paramsParam = new ParamBuilder()
                .withName("params")
                .withNewValue(objectMapper.asJson(component.getSpec().getBuild().getParams()))
                .build();

        pipelineRun = new PipelineRunBuilder(pipelineRun)
                .withNewMetadata()
                .withLabels(labels)
                .withName(build.getMetadata().getName()) // Name should match Build name.
                .withNamespace(build.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewPipelineRef().withName(build.getSpec().getComponent()).endPipelineRef()
                .withWorkspaces(workspace)
                .withParams(componentParam, paramsParam)
                .withNewPodTemplate()
                .withSecurityContext(podSecurityContext)
                .endPodTemplate()
                .endSpec()
                .build();

        if (build != null) {
            OwnerReference ownerReference = new OwnerReferenceBuilder()
                    .withApiVersion(build.getApiVersion())
                    .withKind(build.getKind())
                    .withName(build.getMetadata().getName())
                    .withUid(build.getMetadata().getUid())
                    .withBlockOwnerDeletion(true)
                    .withController(true)
                    .build();

            pipelineRun.getMetadata().setOwnerReferences(Arrays.asList(ownerReference));
        }

        if (serviceAccount.isPresent()) {
            pipelineRun.getSpec().setServiceAccountName(serviceAccount.get());
        }

        LOG.debug("PipelineRun '{}' created for '{}'", fullPipelineRunName(pipelineRun), build.getFullName());
        return pipelineRun;
    }
}
