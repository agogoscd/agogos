package com.redhat.agogos.webhooks.k8s.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.errors.ValidationException;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentHandlerSpec;
import com.redhat.agogos.core.v1alpha1.Handler;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.tekton.pipeline.v1beta1.Task;
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
    protected KubernetesSerialization objectMapper;

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

            Handler handler = agogosClient.v1alpha1().handlers().inNamespace(component.getMetadata().getNamespace())
                    .withName(handlerSpec.getHandlerRef().getName()).get();

            if (handler == null) {
                throw new ApplicationException(
                        "Component definition '{}' is not valid: specified Handler '{}' does not exist in the system",
                        component.getFullName(), handlerSpec.getHandlerRef().getName());
            }

            Task task = tektonClient.v1beta1().tasks().inNamespace(component.getMetadata().getNamespace())
                    .withName(handler.getSpec().getTaskRef().getName()).get();

            Set<String> declaredParams = handlerSpec.getParams().keySet();

            // Parameters declared in Tekton Task
            List<String> taskParams = task.getSpec().getParams().stream()
                    .map(w -> w.getName())
                    .collect(Collectors.toList());

            // Parameters declared Component configuration, but not existing in Tekton Task
            List<String> mismatchedParams = declaredParams.stream().filter(p -> !taskParams.contains(p))
                    .collect(Collectors.toList());

            if (mismatchedParams.size() > 0) {
                throw new ApplicationException(
                        "Parameter mismatch in Handler '{}': following parameters do not exist in Tekton Task '{}': {}",
                        handlerSpec.getHandlerRef().getName(), task.getMetadata().getName(), mismatchedParams);
            }

            // Required parameters in Tekton Task
            List<String> requiredParams = task.getSpec().getParams().stream()
                    .filter(p -> p.getDefault() == null)
                    .map(p -> p.getName())
                    .collect(Collectors.toList());

            List<String> missedParams = requiredParams.stream().filter(w -> !declaredParams.contains(w))
                    .collect(Collectors.toList());

            if (missedParams.size() > 0) {
                throw new ApplicationException(
                        "Missing parameters in Handler '{}': following parameters are required to be defined:: {}",
                        handler.getMetadata().getName(), missedParams);
            }

            declaredParams.forEach(p -> {
                Object schema = handler.getSpec().getSchema().getOpenAPIV3Schema().get(p);

                // No schema provided for the parameter
                if (schema == null) {
                    return;
                }

                ValidationData<Void> validation = new ValidationData<>();

                JsonNode schemaNode = objectMapper.convertValue(schema, JsonNode.class);
                JsonNode contentNode = objectMapper.convertValue(handlerSpec.getParams().get(p), JsonNode.class);

                LOG.debug("Component '{}', Handler '{}': validating parameter '{}' with content: '{}' and schema: '{}'",
                        component.getFullName(),
                        handler.getFullName(),
                        p,
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
                                p, message);
                    });

                    throw new ValidationException("Component '{}', Handler '{}' parameter '{}' is not valid: {}",
                            component.getFullName(), handler.getFullName(),
                            p,
                            errorMessages);
                }
            });

            LOG.info("Component '{}' validation: parameters for Handler '{}' are valid!",
                    component.getFullName(), handlerSpec.getHandlerRef().getName());
        });
    }

    private void validateBuilder(Component component) throws ApplicationException {
        LOG.info("Validating Component's '{}' Builder definition", component.getFullName());

        Builder builder = agogosClient.v1alpha1().builders().withName(component.getSpec().getBuild().getBuilderRef().getName())
                .get();

        if (builder == null) {
            throw new MissingResourceException("Selected builder '{}' is not registered in the system",
                    component.getSpec().getBuild().getBuilderRef().getName());
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
