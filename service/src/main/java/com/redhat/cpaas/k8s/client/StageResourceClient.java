package com.redhat.cpaas.k8s.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cpaas.k8s.model.AbstractStage.Phase;
import com.redhat.cpaas.k8s.model.StageResource;
import com.redhat.cpaas.k8s.model.StageResourceList;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;

/**
 * Stage CR client.
 * 
 * This bean is used to interact with the {@link StageResource} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
public class StageResourceClient {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    MixedOperation<StageResource, StageResourceList, Resource<StageResource>> stageResourceClient;

    @PostConstruct
    void init() {
        stageResourceClient = kubernetesClient.customResources(StageResource.class, StageResourceList.class);
    }

    /**
     * Finds {@link StageResource} by name and returns it.
     * 
     * @param name  Name of the stage
     * @param phase Phase of the stage, one of: build, test, delivery
     * @return {@link StageResource} object.
     */
    public StageResource getByName(String name, final Phase phase) {
        ListOptionsBuilder builder = new ListOptionsBuilder()
                .withFieldSelector(String.format("metadata.name=%s", name));

        if (phase != null) {
            builder.withLabelSelector(String.format("cpaas.redhat.com/phase=%s", phase.toString().toLowerCase()));
        }

        StageResourceList stageResources = stageResourceClient.list(builder.build());

        if (stageResources.getItems().isEmpty() || stageResources.getItems().size() > 1) {
            return null;
        }

        return stageResources.getItems().get(0);
    }

    /**
     * Finds {@link StageResource} by name and returns it.
     * 
     * This method searches for stages in any phase (build, test, delivery).
     * 
     * @param name Name of the stage
     * @return {@link StageResource} object.
     */
    public StageResource getByName(final String name) {
        return getByName(name, null);
    }
}
