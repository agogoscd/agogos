package com.redhat.cpaas.k8s.event;

import com.redhat.cpaas.k8s.ResourceLabels;
import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PipelineRunEventSource<T extends HasMetadata> extends AbstractEventSource
        implements ResourceEventHandler<PipelineRun> {
    protected static final Logger LOG = LoggerFactory.getLogger(PipelineRunEventSource.class);

    @Inject
    protected TektonClient tektonClient;

    /**
     * <p>
     * Finds if the owner of the {@link PipelineRun} is known to us. If this is the
     * case, it returns the {@link OwnerReference#getUid()} value.
     * </p>
     * 
     * <p>
     * In case the owner is not specified or is not a known resource
     * <code>null</code> is returned.
     * </p>
     * 
     * @param pipelineRun the {@link PipelineRun} object
     * @return the uid of the owner
     */
    private String findOwnerUid(PipelineRun pipelineRun) {
        List<OwnerReference> owners = pipelineRun.getMetadata().getOwnerReferences();

        if (owners == null || owners.isEmpty()) {
            return null;
        }

        for (OwnerReference owner : owners) {
            if (isOwner(owner)) {
                return owner.getUid();
            }
        }

        return null;
    }

    protected abstract T createResource(PipelineRun pipelineRun);

    private boolean isEventValid(PipelineRun pipelineRun) {
        String pipelineRunName = String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                pipelineRun.getMetadata().getName());

        Map<String, String> labels = pipelineRun.getMetadata().getLabels();

        if (labels == null || labels.isEmpty()) {
            LOG.warn("No labels found for '{}' Tekton PipelineRun, ignoring", pipelineRunName);
            return false;
        }

        String resource = labels.get(ResourceLabels.RESOURCE.getValue());

        if (resource == null) {
            LOG.warn("Tekton PipelineRun '{}' label '{}' is not set, ignoring", pipelineRunName,
                    ResourceLabels.RESOURCE.getValue());
            return false;
        }

        return true;
    }

    protected abstract boolean isOwner(OwnerReference ownerReference);

    public void handleEvent(PipelineRun pipelineRun, boolean isNew) {
        if (!isEventValid(pipelineRun)) {
            return;
        }

        // TODO
        String uid = findOwnerUid(pipelineRun);

        // Proper owner is already set!
        if (uid != null) {
            handleEvent(pipelineRun, uid);
            return;
        }

        if (!isNew) {
            // Only create resources for new events
            return;
        }

        T owner = createResource(pipelineRun);

        if (owner == null) {
            LOG.warn("Unable to create owner for Tekton PipelineRun '{}/{}', ignoring",
                    pipelineRun.getMetadata().getNamespace(), pipelineRun.getMetadata().getName());
            return;
        }

        updateOwnerReference(owner, pipelineRun);
        handleEvent(pipelineRun, owner.getMetadata().getUid());
    }

    private void handleEvent(PipelineRun pipelineRun, String uid) {
        // If there is no status we will skip notifying the controller
        if (pipelineRun.getStatus() == null) {
            return;
        }

        LOG.trace("Handling '{}' event for PipelineRun '{}' with owner '{}'",
                pipelineRun.getStatus().getConditions().get(0).getReason(), pipelineRun.getMetadata().getName(), uid);

        eventHandler.handleEvent(new PipelineRunEvent(pipelineRun, this, uid));
    }

    protected void updateOwnerReference(T owner, PipelineRun pipelineRun) {
        String ownerName = String.format("%s/%s", owner.getMetadata().getNamespace(), owner.getMetadata().getName());

        String pipelineRunName = String.format("%s/%s", pipelineRun.getMetadata().getNamespace(),
                pipelineRun.getMetadata().getName());

        LOG.info("Making {} '{}' owner of the Tekton PipelineRun '{}'", owner.getKind(), ownerName, pipelineRunName);

        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(owner.getApiVersion()) //
                .withKind(owner.getKind()) //
                .withName(owner.getMetadata().getName()) //
                .withUid(owner.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        try {
            tektonClient.v1beta1().pipelineRuns().inNamespace(pipelineRun.getMetadata().getNamespace())
                    .withName(pipelineRun.getMetadata().getName()).edit(new Visitor<ObjectMetaBuilder>() {
                        @Override
                        public void visit(ObjectMetaBuilder omb) {
                            omb.withOwnerReferences(ownerReference);
                        }
                    });
            LOG.info("{} '{}' set as owner of Tekton PipelineRun '{}'", owner.getKind(), ownerName, pipelineRunName);
        } catch (KubernetesClientException e) {
            LOG.error("Could not set {} '{}' as the owner of Tekton PipelineRun '{}'", owner.getKind(), ownerName,
                    pipelineRunName, e);
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

    }

}
