package com.redhat.agogos.k8s.controllers.build;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PipelineRunPrecondition implements Condition<PipelineRun, Build> {
    @Override
    public boolean isMet(DependentResource<PipelineRun, Build> dependentResource, Build build, Context<Build> context) {
        // Reconcile under the following conditions:
        //   1. Create a PipelineRun resource only when the Component has a status of Ready.
        //   2. The dependent PipelineRun resource is deleted when the Build status is Finished.
        //   3. A new PipelineRun is not created when the Build status is Finished.
        var component = context.getSecondaryResource(Component.class);
        return component.map(c -> ResourceStatus.valueOf(c.getStatus().getStatus()) == ResourceStatus.Ready).orElse(false)
                && !build.getStatus().getStatus().equals("Finished");
    }
}