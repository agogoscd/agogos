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
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public interface V1alpha1APIGroup {
    MixedOperation<Build, BuildList, Resource<Build>> builds();

    MixedOperation<Component, ComponentList, Resource<Component>> components();

    MixedOperation<Pipeline, PipelineList, Resource<Pipeline>> pipelines();

    MixedOperation<Run, RunList, Resource<Run>> runs();

    MixedOperation<Trigger, TriggerList, Resource<Trigger>> triggers();
}
