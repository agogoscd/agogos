package com.redhat.cpaas.k8s.webhooks.validator;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.client.StageResourceClient;
import com.redhat.cpaas.k8s.errors.ApplicationException;
import com.redhat.cpaas.k8s.errors.MissingResourceException;
import com.redhat.cpaas.k8s.errors.ValidationException;
import com.redhat.cpaas.k8s.model.AbstractStage.Phase;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.StageResource;

import org.jboss.logging.Logger;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;

public class ComponentValidator extends Validator<ComponentResource> {

    private static final Logger LOG = Logger.getLogger(ComponentValidator.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    StageResourceClient stageResourceClient;

    @Override
    protected void validateResource(ComponentResource resource, AdmissionResponseBuilder responseBuilder) {
        try {
            validateComponent(resource);

            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Component", e);

            responseBuilder.withAllowed(false) //
                    .withStatus(new StatusBuilder() //
                            .withCode(400) //
                            .withMessage(e.getMessage()) //
                            .build());
        }
    }

    private void validateComponent(ComponentResource component) throws ApplicationException {
        StageResource builder = stageResourceClient.getByName(component.getSpec().getBuilder(), Phase.BUILD);

        if (builder == null) {
            throw new MissingResourceException(String.format("Selected builder '%s' is not registered in the system",
                    component.getSpec().getBuilder()));
        }

        LOG.infov("Validating component ''{0}''", component.getMetadata().getName());

        ValidationData<Void> validation = new ValidationData<>();

        JsonNode schemaNode = objectMapper.valueToTree(builder.getSpec().getSchema().getOpenAPIV3Schema());
        JsonNode contentNode = objectMapper.valueToTree(component.getSpec().getData());

        LOG.debugv("Validating component ''{0}'' content: ''{1}'' with schema: ''{2}''",
                component.getMetadata().getName(), contentNode, schemaNode);

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

            errorMessages.forEach(message -> {
                LOG.errorv("Validation error for ''{0}'' component: ''{1}''", component.getMetadata().getName(),
                        message);
            });

            throw new ValidationException(
                    "Component definition '" + component.getMetadata().getName() + "' is not valid", errorMessages);
        }

        LOG.infov("Component ''{0}'' is valid!", component.getMetadata().getName());
    }
}
