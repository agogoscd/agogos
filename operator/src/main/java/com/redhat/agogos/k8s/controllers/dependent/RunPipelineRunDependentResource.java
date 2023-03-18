package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.v1alpha1.Run;

public class RunPipelineRunDependentResource extends AbstractPipelineRunDependentResource<Run> {

    public String resourceName(Run run) {
        return run.getSpec().getPipeline();
    }

}
