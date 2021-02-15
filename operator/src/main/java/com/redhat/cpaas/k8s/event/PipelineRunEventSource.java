package com.redhat.cpaas.k8s.event;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.cpaas.v1alpha1.ComponentBuildResource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ToString
public class PipelineRunEventSource extends AbstractEventSource implements ResourceEventHandler<PipelineRun> {
    private static final Logger LOG = LoggerFactory.getLogger( PipelineRunEventSource.class);

    /**
     * <p>
     * Finds if the owner of the {@link PipelineRun} is a {@link ComponentBuildResource}. If
     * this is the case, it returns the {@link OwnerReference#getUid()} value.
     * </p>
     * 
     * <p>
     * In case the owner is not specified or is not a {@link ComponentBuildResource}
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

        for (OwnerReference owner : owners) {
            if (owner.getApiVersion().equals(HasMetadata.getApiVersion(ComponentBuildResource.class))
                    && owner.getKind().equals(HasMetadata.getKind(ComponentBuildResource.class))) {
                return owner.getUid();
            }
        }

        return null;
    }

    private void handleEvent(PipelineRun pipelineRun) {
        LOG.trace("Event received for PipelineRun '{}'", pipelineRun.getMetadata().getName());

        String uid = findBuildOwnerUid(pipelineRun);

        if (uid != null) {
            LOG.trace("Handling event for PipelineRun '{}' with owner '{}'", pipelineRun.getMetadata().getName(),
                    uid);
            eventHandler.handleEvent(new PipelineRunEvent(pipelineRun, this, uid));
        }

        LOG.trace("Ignoring event for PipelineRun '{}'", pipelineRun.getMetadata().getName());
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
        LOG.debug("PipelineRun '{}' deleted", pipelineRun.getMetadata().getName());
        handleEvent(pipelineRun);
    }

}
