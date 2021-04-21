package com.redhat.agogos.k8s.webhooks.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.errors.ValidationException;
import com.redhat.agogos.k8s.client.BuilderClient;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ComponentValidator extends Validator<Component> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentValidator.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    BuilderClient builderClient;

    @Override
    protected void validateResource(Component resource, AdmissionResponseBuilder responseBuilder) {
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

    private void validateComponent(Component component) throws ApplicationException {
        LOG.info("Validating component '{}'", component.getNamespacedName());

        Builder builder = builderClient.getByName(component.getSpec().getBuilderRef().get("name"));

        if (builder == null) {
            throw new MissingResourceException("Selected builder '{}' is not registered in the system",
                    component.getSpec().getBuilderRef().get("name"));
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
