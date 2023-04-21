package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.controllers.dependent.RunPipelineRunDependentResource;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = RunPipelineRunDependentResource.class) })
public class RunController extends AbstractRunController<Run> {

    private static final Logger LOG = LoggerFactory.getLogger(RunController.class);

    @Override
    protected Pipeline parentResource(Run run, Context<Run> context) {
        Pipeline pipeline = agogosClient.v1alpha1().pipelines().inNamespace(run.getMetadata().getNamespace())
                .withName(run.getSpec().getPipeline()).get();

        if (pipeline == null) {
            throw new ApplicationException("Could not find Pipeline '{}' in namespace '{}'",
                    run.getSpec().getPipeline(), run.getMetadata().getNamespace());
        }

        return pipeline;
    }

}
