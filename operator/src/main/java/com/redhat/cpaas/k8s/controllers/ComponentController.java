package com.redhat.cpaas.k8s.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.k8s.errors.ApplicationException;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.ComponentResource.ComponentStatus;
import com.redhat.cpaas.k8s.model.ComponentResource.Status;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;

@Controller
public class ComponentController implements ResourceController<ComponentResource> {

    private static final Logger LOG = Logger.getLogger(ComponentController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ComponentResourceClient componentResourceClient;

    @Inject
    TektonResourceClient tektonResourceClient;

    /**
     * Updates {@link ComponentResource.ComponentStatus} of the particular
     * {@link ComponentResource}.
     * 
     * This is useful when the are hooks executed which influence the ability of
     * usage of the Component.
     * 
     * @param component {@link ComponentResource} object
     * @param status    One of available statuses
     * @param reason    Description of the reason for last status change
     */
    private void setStatus(final ComponentResource component, final Status status, final String reason) {
        ComponentStatus componentStatus = component.getStatus();

        componentStatus.setStatus(String.valueOf(status));
        componentStatus.setReason(reason);
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
    }

    private void createPipeline(ComponentResource component) {
        try {
            LOG.debugv("Preparing pipeline for component ''{0}''", component.getMetadata().getName());

            tektonResourceClient.createPipeline(component);

            setStatus(component, Status.Initializing, "Preparing pipeline");

            LOG.infov("Pipeline for component ''{0}'' updated", component.getMetadata().getName());
        } catch (ApplicationException e) {
            LOG.errorv(e, "Error occurred while creating pipeline for component ''{0}''",
                    component.getMetadata().getName());

            setStatus(component, Status.Failed, "Could not create component pipeline");

        }
    }

    @Override
    public DeleteControl deleteResource(ComponentResource component, Context<ComponentResource> context) {
        LOG.infov("Removing component ''{0}''", component.getMetadata().getName());
        return DeleteControl.DEFAULT_DELETE;
    }

    public UpdateControl<ComponentResource> onResourceUpdate(ComponentResource component,
            Context<ComponentResource> context) {
        LOG.infov("Component ''{0}'' modified", component.getMetadata().getName());

        // TODO: Handle component updates

        switch (Status.valueOf(component.getStatus().getStatus())) {
            case New:
                createPipeline(component);
                // TODO
                // Add handling of additional hooks required to be run before the
                // component can be built

                setStatus(component, Status.Ready, "");
                return UpdateControl.updateStatusSubResource(component);
            // case Initializing:
            // // TODO
            // // This is not how it should be, we need to find a way to watch resources to
            // // make the component ready when it is ready
            // setStatus(component, Status.Ready, "");
            // return UpdateControl.updateStatusSubResource(component);
            default:
                break;
        }

        return UpdateControl.noUpdate();
    }

    public UpdateControl<ComponentResource> onEvent(ComponentResource resource, Context<ComponentResource> context) {
        return UpdateControl.updateStatusSubResource(resource);
    }

    @Override
    public UpdateControl<ComponentResource> createOrUpdateResource(ComponentResource resource,
            Context<ComponentResource> context) {
        final var customResourceEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
        if (customResourceEvent.isPresent()) {
            return onResourceUpdate(resource, context);
        }
        return onEvent(resource, context);
    }

}
