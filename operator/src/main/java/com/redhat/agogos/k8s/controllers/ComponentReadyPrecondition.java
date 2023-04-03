package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;

import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ComponentReadyPrecondition implements Condition<PipelineRun, Build> {
    @Override
    public boolean isMet(Build build, PipelineRun secondary, Context<Build> context) {
        var component = context.getSecondaryResource(Component.class);
        return component.map(c -> ResourceStatus.valueOf(c.getStatus().getStatus()) == ResourceStatus.Ready).orElse(false);
    }
}