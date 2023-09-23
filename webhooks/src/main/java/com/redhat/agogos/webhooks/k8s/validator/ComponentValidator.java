package com.redhat.agogos.webhooks.k8s.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.errors.ValidationException;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec.BuilderRef;
import com.redhat.agogos.core.v1alpha1.ComponentHandlerSpec;
import com.redhat.agogos.core.v1alpha1.Handler;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ComponentValidator extends Validator<Component> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentValidator.class);

    @Inject
    AgogosEnvironment agogosEnv;

    @Override
    protected void validateResource(Component component, AdmissionResponseBuilder responseBuilder) {
        try {
            validateBuilder(component);
            validateTaskNames(component);

            validateHandlerParameters(component.getSpec().getPre(), component);
            validateHandlerParameters(component.getSpec().getPost(), component);

            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Component", e);

            responseBuilder.withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(e.getCode())
                            .withMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * <p>
     * Validates correctness of passed parameters to Handlers.
     * </p>
     * 
     * @param component
     * @throws ApplicationException
     */
    private void validateHandlerParameters(List<ComponentHandlerSpec> handlers, Component component)
            throws ApplicationException {

        handlers.stream().forEach(handlerSpec -> {
            LOG.info("Component '{}' validation: validating parameters for Handler '{}'",
                    component.getFullName(), handlerSpec.getHandlerRef().getName());

            Handler handler = kubernetesFacade.get(
                    Handler.class,
                    component.getMetadata().getNamespace(),
                    handlerSpec.getHandlerRef().getName());
            if (handler == null) {
                throw new ApplicationException(
                        "Component definition '{}' is not valid: specified Handler '{}' does not exist in the system",
                        component.getFullName(), handlerSpec.getHandlerRef().getName());
            }

            handlerSpec.getParams().stream().forEach(p -> {
                String name = p.getName();
                String value = p.getValue().getStringVal();
                Object schema = handler.getSpec().getSchema().getOpenAPIV3Schema().get(name);

                // No schema provided for the parameter
                if (schema == null) {
                    return;
                }

                ValidationData<Void> validation = new ValidationData<>();

                JsonNode schemaNode = objectMapper.convertValue(schema, JsonNode.class);
                JsonNode contentNode = objectMapper.convertValue(value, JsonNode.class);

                LOG.debug("Component '{}', Handler '{}': validating parameter '{}' with content: '{}' and schema: '{}'",
                        component.getFullName(),
                        handler.getFullName(),
                        name,
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
                        LOG.error("Component '{}', Handler '{}' parameter '{}' validation error: {}", component.getFullName(),
                                handler.getFullName(),
                                name, message);
                    });

                    throw new ValidationException("Component '{}', Handler '{}' parameter '{}' is not valid: {}",
                            component.getFullName(), handler.getFullName(),
                            name,
                            errorMessages);
                }
            });

            LOG.info("Component '{}' validation: parameters for Handler '{}' are valid!",
                    component.getFullName(), handlerSpec.getHandlerRef().getName());
        });
    }

    private void validateBuilder(Component component) throws ApplicationException {
        LOG.info("Validating Component's '{}' Builder definition", component.getFullName());

        BuilderRef builderRef = component.getSpec().getBuild().getBuilderRef();
        String name = builderRef.getName();
        String namespace = (builderRef.getNamespace() != null ? builderRef.getNamespace() : agogosEnv.getRunningNamespace());
        if (!namespace.equals(agogosEnv.getRunningNamespace()) && !namespace.equals(component.getMetadata().getNamespace())) {
            throw new ApplicationException("Invalid namespace '{}' specified for builder '{}'", namespace, name);
        }

        Builder builder = kubernetesFacade.get(Builder.class, namespace, name);
        if (builder == null) {
            throw new MissingResourceException("Selected builder '{}' is not registered in the namespace '{}'",
                    name, namespace);
        }

        ValidationData<Void> validation = new ValidationData<>();

        JsonNode schemaNode = objectMapper.convertValue(builder.getSpec().getSchema().getOpenAPIV3Schema(), JsonNode.class);
        JsonNode contentNode = objectMapper.convertValue(component.getSpec().getBuild().getParams(), JsonNode.class);

        LOG.debug("Validating component '{}' content: '{}' with schema: '{}'", component.getFullName(),
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
                LOG.error("Validation error for '{}' component: '{}'", component.getFullName(), message);
            });

            throw new ValidationException("Component definition '{}' is not valid: {}", component.getFullName(),
                    errorMessages);
        }

        LOG.info("Component's '{}' Builder definition is valid!", component.getFullName());
    }

    private void validateTaskNames(Component component) throws ApplicationException {
        List<String> names = component.getSpec().getPre().stream().map(s -> s.getHandlerRef().getName())
                .collect(Collectors.toList());
        names.addAll(component.getSpec().getPost().stream().map(s -> s.getHandlerRef().getName()).collect(Collectors.toList()));
        names.add(component.getSpec().getBuild().getBuilderRef().getName());

        Set<String> duplicates = names.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().filter(m -> m.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());

        if (duplicates.size() > 0) {
            throw new ValidationException("Component definition '{}' contains duplicate handler names: {}",
                    component.getFullName(),
                    String.join(", ", duplicates));
        }
    }
}
