package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.PipelineClient;
import com.redhat.agogos.k8s.event.RunEventSource;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(generationAwareEventProcessing = false)
public class PipelineRunController extends AbstractController<Run> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunController.class);

    @Inject
    PipelineClient pipelineClient;

    @Inject
    RunEventSource pipelineRunEventSource;

    @Override
    public DeleteControl deleteResource(Run run, Context<Run> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("pipeline", pipelineRunEventSource);
    }

    @Override
    protected AgogosResource<?, ?> parentResource(Run pipelineRun) {
        Pipeline pipeline = pipelineClient.getByName(pipelineRun.getSpec().getPipeline(),
                pipelineRun.getMetadata().getNamespace());

        if (pipeline == null) {
            throw new ApplicationException("Could not find Pipeline with name '{}' in namespace '{}'",
                    pipelineRun.getSpec().getPipeline(), pipelineRun.getMetadata().getNamespace());
        }

        return pipeline;
    }

}
