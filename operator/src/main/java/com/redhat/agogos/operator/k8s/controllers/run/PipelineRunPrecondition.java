package com.redhat.agogos.operator.k8s.controllers.run;

import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Optional;

public class PipelineRunPrecondition implements Condition<PipelineRun, Run> {

    Optional<Boolean> retainResources = ConfigProvider.getConfig().getOptionalValue("agogos.retain-resources", Boolean.class);

    @Override
    public boolean isMet(DependentResource<PipelineRun, Run> dependentResource, Run run, Context<Run> context) {
        // Reconcile under the following conditions:
        //   1. Create a PipelineRun resource only when the Pipeline has a status of Ready.
        //   2. The dependent PipelineRun resource is deleted when the Run status is Finished (unless retainResources
        //      is set to true).
        //   3. A new PipelineRun is not created when the Run status is Finished.
        var pipeline = context.getSecondaryResource(Pipeline.class);
        return retainResources.orElse(false) ||
                (pipeline.map(c -> c.getStatus().getStatus() == ResourceStatus.READY).orElse(false) &&
                        run.getStatus().getStatus() != ResultableResourceStatus.FINISHED);
    }
}