package com.redhat.agogos.k8s.event;

import com.redhat.agogos.k8s.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@ToString
public class PipelineRunEventHandler implements ResourceEventHandler<PipelineRun> {

    protected static final Logger LOG = LoggerFactory.getLogger(PipelineRunEventHandler.class);

    @Inject
    BuildEventSource buildEventSource;

    @Inject
    RunEventSource runEventSource;

    private void handleEvent(PipelineRun pipelineRun, boolean isNew) {
        String type = pipelineRun.getMetadata().getLabels().get(Resource.RESOURCE.getLabel());

        switch (Resource.fromType(type)) {
            case COMPONENT:
                buildEventSource.handleEvent(Resource.COMPONENT, pipelineRun, isNew);
                break;
            case PIPELINE:
                runEventSource.handleEvent(Resource.PIPELINE, pipelineRun, isNew);
            default:
                break;
        }
    }

    @Override
    public void onAdd(PipelineRun pipelineRun) {
        handleEvent(pipelineRun, true);
    }

    @Override
    public void onUpdate(PipelineRun oldPipelineRun, PipelineRun pipelineRun) {
        handleEvent(pipelineRun, false);
    }

    @Override
    public void onDelete(PipelineRun pipelineRun, boolean deletedFinalStateUnknown) {
        LOG.debug("PipelineRun '{}' deleted from '{}' namespace, ignoring", pipelineRun.getMetadata().getName(),
                pipelineRun.getMetadata().getNamespace());

    }

}
