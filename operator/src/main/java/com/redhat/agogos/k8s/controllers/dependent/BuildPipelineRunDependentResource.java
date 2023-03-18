package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.v1alpha1.Build;

public class BuildPipelineRunDependentResource extends AbstractPipelineRunDependentResource<Build> {

    public String resourceName(Build build) {
        return build.getSpec().getComponent();
    }
}
