package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.BuildList;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentList;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.PipelineList;
import com.redhat.agogos.v1alpha1.Run;
import com.redhat.agogos.v1alpha1.RunList;
import com.redhat.agogos.v1alpha1.TriggerList;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class V1alpha1APIGroupClient implements V1alpha1APIGroup {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public MixedOperation<Build, BuildList, Resource<Build>> builds() {
        return kubernetesClient.customResources(Build.class, BuildList.class);
    }

    @Override
    public MixedOperation<Component, ComponentList, Resource<Component>> components() {
        return kubernetesClient.customResources(Component.class, ComponentList.class);
    }

    @Override
    public MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pipelines() {
        return kubernetesClient.customResources(Pipeline.class, PipelineList.class);
    }

    @Override
    public MixedOperation<Run, RunList, Resource<Run>> runs() {
        return kubernetesClient.customResources(Run.class, RunList.class);
    }

    @Override
    public MixedOperation<Trigger, TriggerList, Resource<Trigger>> triggers() {
        return kubernetesClient.customResources(Trigger.class, TriggerList.class);
    }

}
