package com.redhat.cpaas.k8s.controllers;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.cpaas.k8s.model.BuildResource;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import lombok.ToString;

@ApplicationScoped
@ToString
public class PipelineRunEventSource extends AbstractEventSource implements ResourceEventHandler<PipelineRun> {
    private static final Logger LOG = Logger.getLogger(PipelineRunEventSource.class);

    /**
     * <p>
     * Finds if the owner of the {@link PipelineRun} is a {@link BuildResource}. If
     * this is the case, it returns the {@link OwnerReference#getUid()} value.
     * </p>
     * 
     * <p>
     * In case the owner is not specified or is not a {@link BuildResource}
     * instance, <code>null</code> is returned.
     * </p>
     * 
     * @param pipelineRun the {@link PipelineRun} object
     * @return the uid of the owner
     */
    private String findBuildOwnerUid(PipelineRun pipelineRun) {
        List<OwnerReference> owners = pipelineRun.getMetadata().getOwnerReferences();

        if (owners == null) {
            return null;
        }

        // TODO: Handle API version!
        for (OwnerReference owner : owners) {
            if (owner.getApiVersion().equals("cpaas.redhat.com/v1alpha1")
                    && owner.getKind().equals(BuildResource.KIND)) {
                return owner.getUid();
            }
        }

        return null;
    }

    private void handleEvent(PipelineRun pipelineRun) {
        LOG.tracev("Event received for PipelineRun ''{0}''", pipelineRun.getMetadata().getName());

        String uid = findBuildOwnerUid(pipelineRun);

        if (uid != null) {
            LOG.tracev("Handling event for PipelineRun ''{0}'' with owner ''{1}''", pipelineRun.getMetadata().getName(),
                    uid);
            eventHandler.handleEvent(new PipelineRunEvent(pipelineRun, this, uid));
        }

        LOG.tracev("Ignoring event for PipelineRun ''{0}''", pipelineRun.getMetadata().getName());
    }

    @Override
    public void onAdd(PipelineRun pipelineRun) {
        handleEvent(pipelineRun);
    }

    @Override
    public void onUpdate(PipelineRun oldPipelineRun, PipelineRun pipelineRun) {
        handleEvent(pipelineRun);
    }

    @Override
    public void onDelete(PipelineRun pipelineRun, boolean deletedFinalStateUnknown) {
        LOG.debugv("PipelineRun ''{0}'' deleted", pipelineRun.getMetadata().getName());
        handleEvent(pipelineRun);
    }

}
