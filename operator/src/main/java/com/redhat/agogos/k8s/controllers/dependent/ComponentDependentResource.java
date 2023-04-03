package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

public class ComponentDependentResource extends KubernetesDependentResource<Component, Build> {

    public ComponentDependentResource() {
        super(Component.class);
    }
}
