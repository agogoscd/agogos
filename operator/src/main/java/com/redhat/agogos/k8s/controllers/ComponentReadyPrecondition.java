package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class ComponentReadyPrecondition implements Condition<Component, Build> {
    @Override
    public boolean isMet(Build build, Component secondary, Context<Build> context) {
        var component =  context.getSecondaryResource(Component.class);
        return component.map(c->"ready".equals(c.getStatus().getStatus())).orElse(false);
    }
}
