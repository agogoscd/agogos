package com.redhat.cpaas.k8s.event;

import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.k8s.ResourceLabels;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PipelineRunEventHandler implements ResourceEventHandler<PipelineRun> {
    protected static final Logger LOG = LoggerFactory.getLogger(PipelineRunEventHandler.class);

    @Inject
    BuildPipelineRunEventSource buildPipelineRunEventSource;

    @Inject
    PipelinePipelineRunEventSource pipelinePipelineRunEventSource;

    @SuppressWarnings("rawtypes")
    private PipelineRunEventSource getEventSource(PipelineRun pipelineRun) {

        String resource = pipelineRun.getMetadata().getLabels().get(ResourceLabels.RESOURCE.getValue());

        switch (resource) {
            case "component":
                return buildPipelineRunEventSource;
            case "pipeline":
                return pipelinePipelineRunEventSource;
            default:
                throw new ApplicationException("Unsupported resource type: '{}'", resource);
        }
    }

    @Override
    public void onAdd(PipelineRun pipelineRun) {
        getEventSource(pipelineRun).handleEvent(pipelineRun, true);
    }

    @Override
    public void onUpdate(PipelineRun oldPipelineRun, PipelineRun pipelineRun) {
        getEventSource(pipelineRun).handleEvent(pipelineRun, false);
    }

    @Override
    public void onDelete(PipelineRun pipelineRun, boolean deletedFinalStateUnknown) {
        LOG.debug("PipelineRun '{}' deleted from '{}' namespace, ignoring", pipelineRun.getMetadata().getName(),
                pipelineRun.getMetadata().getNamespace());

    }

}
