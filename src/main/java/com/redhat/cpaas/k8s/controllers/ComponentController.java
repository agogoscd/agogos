package com.redhat.cpaas.k8s.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.ApplicationException;
import com.redhat.cpaas.MissingResourceException;
import com.redhat.cpaas.ValidationException;
import com.redhat.cpaas.k8s.client.BuilderResourceClient;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.k8s.model.BuilderResource;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.ComponentResource.ComponentStatus;
import com.redhat.cpaas.k8s.model.ComponentResource.Status;

import org.jboss.logging.Logger;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

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

    @Inject
    BuilderResourceClient builderResourceClient;

    @Inject
    ObjectMapper objectMapper;

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

    private void validate(ComponentResource component) throws ApplicationException {
        BuilderResource builder = builderResourceClient.getByName(component.getSpec().getBuilder());

        if (builder == null) {
            throw new MissingResourceException(String.format("Selected builder '%s' is not registered in the system",
                    component.getSpec().getBuilder()));
        }

        LOG.infov("Validating component ''{0}''", component.getMetadata().getName());

        ValidationData<Void> validation = new ValidationData<>();

        JsonNode schemaNode = objectMapper.valueToTree(builder.getSpec().getSchema().getOpenAPIV3Schema());
        JsonNode contentNode = objectMapper.valueToTree(component.getSpec().getData());

        SchemaValidator schemaValidator;

        try {
            schemaValidator = new SchemaValidator(null, schemaNode);
        } catch (ResolutionException e) {
            e.printStackTrace();
            throw new ApplicationException("Could not instantiate validator", e);
        }

        schemaValidator.validate(contentNode, validation);

        if (!validation.isValid()) {
            List<String> errorMessages = validation.results().items().stream()
                    .map(item -> item.message().replaceAll("\\.+$", "")).collect(Collectors.toList());
            throw new ValidationException("Component definition is not valid", errorMessages);
        }

        LOG.infov("Component ''{0}'' is valid!", component.getMetadata().getName());
    }

    @Override
    public DeleteControl deleteResource(ComponentResource component, Context<ComponentResource> context) {
        LOG.infov("Removing component ''{0}''", component.getMetadata().getName());
        return DeleteControl.DEFAULT_DELETE;
    }

    public UpdateControl<ComponentResource> onResourceUpdate(ComponentResource component,
            Context<ComponentResource> context) {
        LOG.infov("Component ''{0}'' modified", component.getMetadata().getName());

        // TODO: This should be done before the request is accepted
        // Any incorrect data should not be allowed
        // ValidatingAdmissionWebhook
        // https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/#validatingadmissionwebhook
        try {
            validate(component);
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(component, Status.Failed, "Validation failed");
            return UpdateControl.updateStatusSubResource(component);
        }

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
