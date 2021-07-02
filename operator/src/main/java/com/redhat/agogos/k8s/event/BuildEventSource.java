package com.redhat.agogos.k8s.event;

import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.BuildClient;
import com.redhat.agogos.v1alpha1.Build;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import lombok.ToString;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@ToString
public class BuildEventSource extends AbstractTektonEventSource<Build> {
    @Inject
    BuildClient componentBuildClient;

    @Override
    protected Build createResource(PipelineRun pipelineRun) {
        String pipelineRunName = String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                pipelineRun.getMetadata().getName());

        Map<String, String> labels = pipelineRun.getMetadata().getLabels();

        String component = labels.get(Resource.COMPONENT.getLabel());

        if (component == null) {
            LOG.warn("Tekton PipelineRun '{}' for Component resource has no '{}' label specified, ignoring",
                    pipelineRunName, Resource.COMPONENT.getLabel());
            return null;
        }

        LOG.info("No Build associated with Tekton PipelineRun '{}' found, creating new one", pipelineRunName);

        Build build = new Build();

        Map<String, String> buildLabels = new HashMap<>();
        buildLabels.put(Resource.PIPELINERUN.getLabel(), pipelineRun.getMetadata().getName());

        build.getMetadata().setGenerateName(component + "-");
        build.getMetadata().setLabels(buildLabels);
        build.getSpec().setComponent(component);

        build = componentBuildClient.create(build, pipelineRun.getMetadata().getNamespace());

        LOG.info("Build '{}' created out of an existing Tekton PipelineRun '{}'", build.getFullName(),
                pipelineRunName);

        return build;
    }

    @Override
    protected boolean isOwner(OwnerReference owner) {
        return owner.getKind().equals(HasMetadata.getKind(Build.class))
                && owner.getApiVersion().equals(HasMetadata.getApiVersion(Build.class));
    }
}
