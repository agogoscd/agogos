package com.redhat.cpaas.k8s.event;

import com.redhat.cpaas.k8s.ResourceLabels;
import com.redhat.cpaas.k8s.client.PipelineRunClient;
import com.redhat.cpaas.v1alpha1.PipelineRunResource;
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
public class PipelinePipelineRunEventSource extends PipelineRunEventSource<PipelineRunResource> {

    @Inject
    PipelineRunClient pipelineRunClient;

    @Override
    protected PipelineRunResource createResource(PipelineRun pipelineRun) {
        String pipelineRunName = String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                pipelineRun.getMetadata().getName());

        Map<String, String> labels = pipelineRun.getMetadata().getLabels();

        String pipeline = labels.get(ResourceLabels.PIPELINE.getValue());

        if (pipeline == null) {
            LOG.warn("Tekton PipelineRun '{}' for Pipeline resource has no '{}' label specified, ignoring",
                    pipelineRunName, ResourceLabels.PIPELINE.getValue());
            return null;
        }

        LOG.info("No PipelineRun associated with Tekton PipelineRun '{}' found, creating new one", pipelineRunName);

        PipelineRunResource run = new PipelineRunResource();
        Map<String, String> runLabels = new HashMap<>();
        runLabels.put(ResourceLabels.PIPELINE_RUN.getValue(), pipelineRun.getMetadata().getName());

        run.getMetadata().setGenerateName(pipeline + "-");
        run.getMetadata().setLabels(runLabels);
        run.getSpec().setPipeline(pipeline);

        run = pipelineRunClient.create(run, pipelineRun.getMetadata().getNamespace());

        LOG.info("PipelineRun '{}' created out of an existing Tekton PipelineRun '{}'", run.getNamespacedName(),
                pipelineRunName);

        return run;
    }

    @Override
    protected boolean isOwner(OwnerReference owner) {
        return owner.getKind().equals(HasMetadata.getKind(PipelineRunResource.class))
                && owner.getApiVersion().equals(HasMetadata.getApiVersion(PipelineRunResource.class));
    }
}
