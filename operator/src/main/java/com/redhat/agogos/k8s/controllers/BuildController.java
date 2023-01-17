package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.k8s.event.BuildEventSource;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class BuildController extends AbstractController<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);

    @Inject
    AgogosClient agogosClient;

    @Inject
    BuildEventSource buildEventSource;

    /**
     * <p>
     * Register the {@link io.fabric8.tekton.pipeline.v1alpha1.PipelineRun} event
     * source so that we can receive events from PipelineRun's that are related to
     * {@link Component}'s.
     * </p>
     */
    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Build> context) {
        return List.of(buildEventSource);
    }

    /**
     * <p>
     * Method triggered when a {@link Build} is removed from the cluster.
     * </p>
     * 
     * @param build {@link Build}
     * @param context {@link Context}
     * @return {@link DeleteControl}
     */
    @Override
    public DeleteControl cleanup(Build build, Context context) {
        return DeleteControl.defaultDelete();
    }

    @Override
    protected AgogosResource<?, ?> parentResource(Build build) {
        LOG.debug("Finding parent resource for Build '{}'", build.getFullName());

        Component component = agogosClient.v1alpha1().components().inNamespace(build.getMetadata().getNamespace())
                .withName(build.getSpec().getComponent()).get();

        if (component == null) {
            throw new ApplicationException("Could not find Component with name '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        return component;
    }

}
