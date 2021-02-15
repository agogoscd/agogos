package com.redhat.cpaas.k8s.webhooks.validator;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.errors.ValidationException;
import com.redhat.cpaas.k8s.client.StageResourceClient;
import com.redhat.cpaas.v1alpha1.AbstractStage.Phase;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import com.redhat.cpaas.v1alpha1.StageResource;

import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentValidator extends Validator<ComponentResource> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentValidator.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    StageResourceClient stageResourceClient;

    /**
     * We have to register the resource within {@link KubernetesDeserializer} so
     * that it can deserialize attached object as part of the incoming
     * AdmissionReview.
     */
    void onStart(@Observes StartupEvent ev) {
        KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(ComponentResource.class),
                HasMetadata.getKind(ComponentResource.class), ComponentResource.class);
    }

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
        LOG.info("Validating component '{}'", component.getNamespacedName());

        StageResource builder = stageResourceClient.getByName(component.getSpec().getBuilder(), Phase.BUILD);

        if (builder == null) {
            throw new MissingResourceException("Selected builder '{}' is not registered in the system",
                    component.getSpec().getBuilder());
        }

        ValidationData<Void> validation = new ValidationData<>();

        JsonNode schemaNode = objectMapper.valueToTree(builder.getSpec().getSchema().getOpenAPIV3Schema());
        JsonNode contentNode = objectMapper.valueToTree(component.getSpec().getData());

        LOG.debug("Validating component '{}' content: '{}' with schema: '{}'", component.getNamespacedName(),
                contentNode, schemaNode);

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
                LOG.error("Validation error for '{}' component: '{}'", component.getNamespacedName(), message);
            });

            throw new ValidationException("Component definition '{}' is not valid: {}", component.getNamespacedName(),
                    errorMessages);
        }

        LOG.info("Component '{}' is valid!", component.getNamespacedName());
    }
}
