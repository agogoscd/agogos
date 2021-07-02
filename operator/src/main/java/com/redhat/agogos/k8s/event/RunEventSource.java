package com.redhat.agogos.k8s.event;

import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.RunClient;
import com.redhat.agogos.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import lombok.ToString;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@ToString
public class RunEventSource extends AbstractTektonEventSource<Run> {

    @Inject
    RunClient pipelineRunClient;

    @Override
    protected Run createResource(io.fabric8.tekton.pipeline.v1beta1.PipelineRun pipelineRun) {
        String pipelineRunName = String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                pipelineRun.getMetadata().getName());

        Map<String, String> labels = pipelineRun.getMetadata().getLabels();

        String pipeline = labels.get(Resource.PIPELINE.getLabel());

        if (pipeline == null) {
            LOG.warn("Tekton PipelineRun '{}' for Pipeline resource has no '{}' label specified, ignoring",
                    pipelineRunName, Resource.PIPELINE.getLabel());
            return null;
        }

        LOG.info("No PipelineRun associated with Tekton PipelineRun '{}' found, creating new one", pipelineRunName);

        Run run = new Run();
        Map<String, String> runLabels = new HashMap<>();
        runLabels.put(Resource.PIPELINERUN.getLabel(), pipelineRun.getMetadata().getName());

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
        return owner.getKind().equals(HasMetadata.getKind(Run.class))
                && owner.getApiVersion().equals(HasMetadata.getApiVersion(Run.class));
    }
}
