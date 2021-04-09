package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.AbstractStage.Phase;
import com.redhat.agogos.v1alpha1.Stage;
import com.redhat.agogos.v1alpha1.StageList;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Stage CR client.
 * 
 * This bean is used to interact with the {@link Stage} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
@RegisterForReflection
public class StageClient {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    MixedOperation<Stage, StageList, Resource<Stage>> stageResourceClient;

    @PostConstruct
    void init() {
        stageResourceClient = kubernetesClient.customResources(Stage.class, StageList.class);
    }

    /**
     * Finds {@link Stage} by name and returns it.
     * 
     * @param name Name of the stage
     * @param phase Phase of the stage, one of: build, test, delivery
     * @return {@link Stage} object.
     */
    public Stage getByName(String name, final Phase phase) {
        ListOptionsBuilder builder = new ListOptionsBuilder()
                .withFieldSelector(String.format("metadata.name=%s", name));

        if (phase != null) {
            builder.withLabelSelector(String.format("agogos.redhat.com/phase=%s", phase.toString().toLowerCase()));
        }

        StageList stageResources = stageResourceClient.list(builder.build());

        if (stageResources.getItems().isEmpty() || stageResources.getItems().size() > 1) {
            return null;
        }

        return stageResources.getItems().get(0);
    }

    /**
     * Finds {@link Stage} by name and returns it.
     * 
     * This method searches for stages in any phase (build, test, delivery).
     * 
     * @param name Name of the stage
     * @return {@link Stage} object.
     */
    public Stage getByName(final String name) {
        return getByName(name, null);
    }
}
