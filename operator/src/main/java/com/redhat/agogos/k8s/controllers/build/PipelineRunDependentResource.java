package com.redhat.agogos.k8s.controllers.build;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.controllers.AbstractDependentResource;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.WorkspaceMapping;
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

    @Override
    protected PipelineRun desired(Build resource, Context<Build> context) {
        PipelineRun pipelineRun = new PipelineRun();
        Optional<PipelineRun> optional = context.getSecondaryResource(PipelineRun.class);
        if (!optional.isEmpty()) {
            pipelineRun = optional.get();
            LOG.debug("{} '{}', using existing PipelineRun '{}'", resource.getKind(),
                    resource.getFullName(), fullPipelineRunName(pipelineRun));
        } else {
            LOG.debug("{} '{}', creating new PipelineRun", resource.getKind(), resource.getFullName());
        }

        WorkspaceBinding workspace = new WorkspaceBindingBuilder()
                .withName(WorkspaceMapping.MAIN_WORKSPACE_NAME)
                .withEmptyDir(new EmptyDirVolumeSource())
                .build();

        Map<String, String> labels = new HashMap<>();
        labels.put(Resource.RESOURCE.getLabel(), parentResource(resource).getKind().toLowerCase());

        PodSecurityContext podSecurityContext = new PodSecurityContextBuilder()
                .withRunAsNonRoot(runAsNonRoot.orElse(true))
                .withRunAsUser(runAsUser.orElse(65532l))
                .build();

        Component component = context.getSecondaryResource(Component.class).get();
        Param param = new ParamBuilder()
                .withName("component")
                .withNewValue(context.getClient().getKubernetesSerialization().asYaml(component))
                .build();

        pipelineRun = new PipelineRunBuilder(pipelineRun)
                .withNewMetadata()
                .withLabels(labels)
                .withName(resource.getMetadata().getName()) // Name should match Build name.
                .withNamespace(resource.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewPipelineRef().withName(resource.getSpec().getComponent()).endPipelineRef()
                .withWorkspaces(workspace)
                .withParams(param)
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

    protected Component parentResource(Build build) {
        LOG.debug("Finding parent Component for Build '{}'", build.getFullName());

        Component component = agogosClient.v1alpha1().components().inNamespace(build.getMetadata().getNamespace())
                .withName(build.getSpec().getComponent()).get();

        if (component == null) {
            throw new ApplicationException("Could not find Component '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        return component;
    }
}
