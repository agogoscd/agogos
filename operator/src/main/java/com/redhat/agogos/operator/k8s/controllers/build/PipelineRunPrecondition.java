package com.redhat.agogos.operator.k8s.controllers.build;

import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Optional;

public class PipelineRunPrecondition implements Condition<PipelineRun, Build> {

    Optional<Boolean> retainResources = ConfigProvider.getConfig().getOptionalValue("agogos.retain-resources", Boolean.class);

    @Override
    public boolean isMet(DependentResource<PipelineRun, Build> dependentResource, Build build, Context<Build> context) {
        // Reconcile under the following conditions:
        //   1. Create a PipelineRun resource only when the Component has a status of Ready.
        //   2. The dependent PipelineRun resource is deleted when the Build status is Finished (unless retainResources
        //      is set to true).
        //   3. A new PipelineRun is not created when the Build status is Finished.
        var component = context.getSecondaryResource(Component.class);
        return retainResources.orElse(false) ||
                (component.map(c -> c.getStatus().getStatus() == ResourceStatus.READY).orElse(false) &&
                        build.getStatus().getStatus() != ResultableResourceStatus.FINISHED);
    }
}
