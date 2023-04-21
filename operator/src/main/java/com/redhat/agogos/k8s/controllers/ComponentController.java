package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.k8s.controllers.dependent.ComponentPipelineDependentResource;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Status;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = ComponentPipelineDependentResource.class) })
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
        Status componentStatus = component.getStatus();
        if (optional.isPresent()) {
            componentStatus.setStatus(String.valueOf(ResourceStatus.Ready));
            componentStatus.setReason("Component is ready");
        } else {
            componentStatus.setStatus(String.valueOf(ResourceStatus.Failed));
            componentStatus.setReason("Could not create Component");
        }
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        LOG.info("Set status for Component '{}' to {}", component.getFullName(), component.getStatus().getStatus());

        return UpdateControl.updateStatus(component);
    }
}
