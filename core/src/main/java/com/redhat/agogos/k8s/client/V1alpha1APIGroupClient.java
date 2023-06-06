package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.BuildList;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.BuilderList;
import com.redhat.agogos.v1alpha1.ClusterStage;
import com.redhat.agogos.v1alpha1.ClusterStageList;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentList;
import com.redhat.agogos.v1alpha1.Group;
import com.redhat.agogos.v1alpha1.GroupList;
import com.redhat.agogos.v1alpha1.Handler;
import com.redhat.agogos.v1alpha1.HandlerList;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.PipelineList;
import com.redhat.agogos.v1alpha1.Run;
import com.redhat.agogos.v1alpha1.RunList;
import com.redhat.agogos.v1alpha1.Stage;
import com.redhat.agogos.v1alpha1.StageList;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import com.redhat.agogos.v1alpha1.triggers.TriggerList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection
public class V1alpha1APIGroupClient implements V1alpha1APIGroup {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    @Produces
    public MixedOperation<Build, BuildList, Resource<Build>> builds() {
        return kubernetesClient.resources(Build.class, BuildList.class);
    }

    @Override
    @Produces
    public MixedOperation<Component, ComponentList, Resource<Component>> components() {
        return kubernetesClient.resources(Component.class, ComponentList.class);
    }

    @Override
    @Produces
    public MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pipelines() {
        return kubernetesClient.resources(Pipeline.class, PipelineList.class);
    }

    @Override
    @Produces
    public MixedOperation<Run, RunList, Resource<Run>> runs() {
        return kubernetesClient.resources(Run.class, RunList.class);
    }

    @Override
    @Produces
    public MixedOperation<Trigger, TriggerList, Resource<Trigger>> triggers() {
        return kubernetesClient.resources(Trigger.class, TriggerList.class);
    }

    @Override
    @Produces
    public MixedOperation<ClusterStage, ClusterStageList, Resource<ClusterStage>> clusterstages() {
        return kubernetesClient.resources(ClusterStage.class, ClusterStageList.class);
    }

    @Override
    @Produces
    public MixedOperation<Stage, StageList, Resource<Stage>> stages() {
        return kubernetesClient.resources(Stage.class, StageList.class);
    }

    @Override
    @Produces
    public MixedOperation<Builder, BuilderList, Resource<Builder>> builders() {
        return kubernetesClient.resources(Builder.class, BuilderList.class);
    }

    @Override
    public MixedOperation<Group, GroupList, Resource<Group>> groups() {
        return kubernetesClient.resources(Group.class, GroupList.class);
    }

    @Override
    public MixedOperation<Handler, HandlerList, Resource<Handler>> handlers() {
        return kubernetesClient.resources(Handler.class, HandlerList.class);
    }

}
