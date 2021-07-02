package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.AbstractStage;
import com.redhat.agogos.v1alpha1.ClusterStage;
import com.redhat.agogos.v1alpha1.ClusterStageList;
import com.redhat.agogos.v1alpha1.Stage;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@RegisterForReflection
public class ClusterStageClient {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    MixedOperation<ClusterStage, ClusterStageList, Resource<ClusterStage>> clusterStageClient;

    @PostConstruct
    void init() {
        clusterStageClient = kubernetesClient.customResources(ClusterStage.class, ClusterStageList.class);
    }

    /**
     * Finds {@link Stage} by name and returns it.
     * 
     * @param name Name of the stage
     * @param phase Phase of the stage, one of: build, test, delivery
     * @return {@link Stage} object.
     */
    public ClusterStage getByName(String name) {
        ListOptionsBuilder builder = new ListOptionsBuilder()
                .withFieldSelector(String.format("metadata.name=%s", name));

        ClusterStageList stageResources = clusterStageClient.list(builder.build());

        if (stageResources.getItems().isEmpty() || stageResources.getItems().size() > 1) {
            return null;
        }

        return stageResources.getItems().get(0);
    }

    public AbstractStage getByName(String name, String kind) {
        ListOptionsBuilder builder = new ListOptionsBuilder()
                .withFieldSelector(String.format("metadata.name=%s", name));

        switch (kind) {
            case "Stage":

                break;

            default:
                break;
        }

        CustomResourceList<? extends AbstractStage> stageResources = clusterStageClient.list(builder.build());

        if (stageResources.getItems().isEmpty() || stageResources.getItems().size() > 1) {
            return null;
        }

        return stageResources.getItems().get(0);
    }
}
