package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.ComponentClient;
import com.redhat.agogos.k8s.event.BuildEventSource;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(generationAwareEventProcessing = false)
public class BuildController extends AbstractController<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);

    @Inject
    ComponentClient componentClient;

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
    public void init(EventSourceManager eventSourceManager) {
        eventSourceManager.registerEventSource("component", buildEventSource);
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
    public DeleteControl deleteResource(Build build, Context<Build> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    protected AgogosResource<?, ?> parentResource(Build build) {
        LOG.debug("Finding parent resource for Build '{}'", build.getFullName());

        Component component = componentClient.getByName(build.getSpec().getComponent(),
                build.getMetadata().getNamespace());

        if (component == null) {
            throw new ApplicationException("Could not find Component with name '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        return component;
    }

}
