package com.redhat.cpaas.k8s.event;

import com.redhat.cpaas.k8s.ResourceLabels;
import com.redhat.cpaas.k8s.client.ComponentBuildResourceClient;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.ToString;

@ApplicationScoped
@ToString
public class BuildPipelineRunEventSource extends PipelineRunEventSource<ComponentBuildResource> {
    @Inject
    ComponentBuildResourceClient componentBuildClient;

    @Override
    protected ComponentBuildResource createResource(PipelineRun pipelineRun) {
        String pipelineRunName = String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                pipelineRun.getMetadata().getName());

        Map<String, String> labels = pipelineRun.getMetadata().getLabels();

        String component = labels.get(ResourceLabels.COMPONENT.getValue());

        if (component == null) {
            LOG.warn("Tekton PipelineRun '{}' for Component resource has no '{}' label specified, ignoring",
                    pipelineRunName, ResourceLabels.COMPONENT.getValue());
            return null;
        }

        LOG.info("No Build associated with Tekton PipelineRun '{}' found, creating new one", pipelineRunName);

        ComponentBuildResource build = new ComponentBuildResource();

        Map<String, String> buildLabels = new HashMap<>();
        buildLabels.put(ResourceLabels.PIPELINE_RUN.getValue(), pipelineRun.getMetadata().getName());

        build.getMetadata().setGenerateName(component + "-");
        build.getMetadata().setLabels(buildLabels);
        build.getSpec().setComponent(component);

        build = componentBuildClient.create(build);

        LOG.info("Build '{}' created out of an existing Tekton PipelineRun '{}'", build.getNamespacedName(),
                pipelineRunName);

        return build;
    }

    @Override
    protected boolean isOwner(OwnerReference owner) {
        return owner.getKind().equals(HasMetadata.getKind(ComponentBuildResource.class))
                && owner.getApiVersion().equals(HasMetadata.getApiVersion(ComponentBuildResource.class));
    }
}
