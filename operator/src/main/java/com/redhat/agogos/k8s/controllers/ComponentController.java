package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.k8s.controllers.dependent.PipelineDependentResource;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Status;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = PipelineDependentResource.class) })
public class ComponentController extends AbstractController<Component> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentController.class);

    /**
     * <p>
     * Main method that is triggered when a change on the {@link Component}
     * object is detected.
     * </p>
     * 
     * @param component {@link Component}
     * @param context {@link Context}
     * @return {@link UpdateControl}
     */
    @Override
    public UpdateControl<Component> reconcile(Component component, Context<Component> context) {
        Optional<Pipeline> optional = context.getSecondaryResource(Pipeline.class);
        if (!optional.isPresent()) {
            LOG.debug("No pipeline for Component '{}' yet, ignoring", component.getFullName());
            return UpdateControl.noUpdate();
        }

        // Set the component status.
        Status componentStatus = component.getStatus();
        componentStatus.setStatus(String.valueOf(ResourceStatus.Ready));
        componentStatus.setReason("Component is ready");
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        LOG.info("Set status for Component '{}' to {}", component.getFullName(), component.getStatus().getStatus());

        return UpdateControl.updateStatus(component);
    }
}
