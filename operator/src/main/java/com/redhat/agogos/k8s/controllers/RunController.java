package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.k8s.event.RunEventSource;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Map;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class RunController extends AbstractController<Run> {

    private static final Logger LOG = LoggerFactory.getLogger(RunController.class);

    @Inject
    AgogosClient agogosClient;

    @Inject
    RunEventSource runEventSource;

    @Override
    public DeleteControl cleanup(Run run, Context<Run> context) {
        return DeleteControl.defaultDelete();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Run> context) {
        return Map.of(EventSourceInitializer.generateNameFor(runEventSource), runEventSource);
    }

    @Override
    protected AgogosResource<?, ?> parentResource(Run pipelineRun) {
        Pipeline pipeline = agogosClient.v1alpha1().pipelines().inNamespace(pipelineRun.getMetadata().getNamespace())
                .withName(pipelineRun.getSpec().getPipeline()).get();

        if (pipeline == null) {
            throw new ApplicationException("Could not find Pipeline with name '{}' in namespace '{}'",
                    pipelineRun.getSpec().getPipeline(), pipelineRun.getMetadata().getNamespace());
        }

        return pipeline;
    }

}
