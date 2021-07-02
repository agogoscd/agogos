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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Controller(generationAwareEventProcessing = false)
public class RunController extends AbstractController<Run> {

    private static final Logger LOG = LoggerFactory.getLogger(RunController.class);

    @Inject
    PipelineClient pipelineClient;

    @Inject
    RunEventSource runEventSource;

    @Override
    public DeleteControl deleteResource(Run run, Context<Run> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("pipeline", runEventSource);
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
