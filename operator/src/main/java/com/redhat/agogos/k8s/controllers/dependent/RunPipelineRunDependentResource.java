package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunPipelineRunDependentResource extends AbstractPipelineRunDependentResource<Run> {

    protected static final Logger LOG = LoggerFactory.getLogger(RunPipelineRunDependentResource.class);

    public String resourceName(Run run) {
        return run.getSpec().getPipeline();
    }

    @Override
    protected Pipeline parentResource(Run run) {
        Pipeline pipeline = agogosClient.v1alpha1().pipelines().inNamespace(run.getMetadata().getNamespace())
                .withName(run.getSpec().getPipeline()).get();

        if (pipeline == null) {
            throw new ApplicationException("Could not find Pipeline '{}' in namespace '{}'",
                    run.getSpec().getPipeline(), run.getMetadata().getNamespace());
        }

        return pipeline;
    }
}
