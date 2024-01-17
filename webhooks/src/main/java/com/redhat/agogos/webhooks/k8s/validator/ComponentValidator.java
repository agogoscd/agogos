package com.redhat.agogos.webhooks.k8s.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.errors.ValidationException;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec.BuilderRef;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.StageEntry;
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

            validateStageParameters(component.getSpec().getPre(), component);
            validateStageParameters(component.getSpec().getPost(), component);

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
     * Validates correctness of passed parameters to Stages.
     * </p>
     * 
     * @param component
     * @throws ApplicationException
     */
    private void validateStageParameters(List<StageEntry> entries, Component component)
            throws ApplicationException {

        entries.stream().forEach(entry -> {
            LOG.info("Component '{}' validation: validating parameters for Stage '{}'",
                    component.getFullName(), entry.getStageRef().getName());

            Stage stage = kubernetesFacade.get(
                    Stage.class,
                    component.getMetadata().getNamespace(),
                    entry.getStageRef().getName());
            if (stage == null) {
                throw new ApplicationException(
                        "Component definition '{}' is not valid: specified Stage '{}' does not exist in the system",
                        component.getFullName(), entry.getStageRef().getName());
            }

            Object schema = stage.getSpec().getSchema().getOpenAPIV3Schema();

            // No schema provided for the parameter
            if (schema == null) {
                LOG.warn("Component '{}' validation: no schema found for Stage '{}'",
                        component.getFullName(), stage.getFullName());
                return;
            }

            ValidationData<Void> validation = new ValidationData<>();

            JsonNode schemaNode = objectMapper.convertValue(schema, JsonNode.class);
            JsonNode contentNode = objectMapper.convertValue(entry.getConfig(), JsonNode.class);

            LOG.debug("Component '{}', Stage '{}': validating content: '{}' and schema: '{}'",
                    component.getFullName(),
                    stage.getFullName(),
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
                    LOG.error("Component '{}', Stage '{}' params validation error: {}", component.getFullName(),
                            stage.getFullName(), message);
                });

                throw new ValidationException("Component '{}', Stage '{}' parameters are not valid: {}",
                        component.getFullName(), stage.getFullName(), errorMessages);
            }

            LOG.info("Component '{}' validation: parameters for Stage '{}' are valid!",
                    component.getFullName(), entry.getStageRef().getName());
        });
    }

    private void validateBuilder(Component component) throws ApplicationException {
        LOG.info("Validating Component's '{}' Builder definition", component.getFullName());

        BuilderRef builderRef = component.getSpec().getBuild().getBuilderRef();
        String name = builderRef.getName();

        // We want to search the Component's namespace first, then fallback to where the Agogos operator is installed
        String componentNamespace = component.getMetadata().getNamespace();
        String agogosNamespace = agogosEnv.getRunningNamespace();
        String builderNamespace = builderRef.getNamespace() != null ? builderRef.getNamespace() : "";

        // We want Components relying on their own Builders (same namespace) or Agogos' ('agogos' namespace)
        if (!builderNamespace.equals("") && !builderNamespace.equals(agogosNamespace)
                && !builderNamespace.equals(componentNamespace)) {
            throw new ApplicationException("Invalid namespace '{}' specified for builder '{}'.", builderNamespace, name);
        }

        // If user defines a namespace, we ensure the Builder is there
        // otherwise we try the Component's namespace and then Agogos' namespace
        Builder builder;
        if (builderNamespace != "") {
            builder = kubernetesFacade.get(Builder.class, builderNamespace, name);
            if (builder == null) {
                throw new MissingResourceException(
                        "Selected builder '{}' is not registered in the selected namespace '{}'",
                        name,
                        builderNamespace);
            }
        } else {
            builder = kubernetesFacade.get(Builder.class, componentNamespace, name);
            if (builder == null) {
                LOG.debug("Builder '{}' not found in the component's namespace '{}'.", name, componentNamespace);
                builder = kubernetesFacade.get(Builder.class, agogosNamespace, name);
                if (builder == null) {
                    throw new MissingResourceException(
                            "Selected builder '{}' is not registered in the namespaces '{}' or '{}'",
                            name,
                            componentNamespace,
                            agogosNamespace);
                }
            }
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
        List<String> names = component.getSpec().getPre().stream().map(s -> s.getStageRef().getName())
                .collect(Collectors.toList());
        names.addAll(component.getSpec().getPost().stream().map(s -> s.getStageRef().getName()).collect(Collectors.toList()));
        names.add(component.getSpec().getBuild().getBuilderRef().getName());

        Set<String> duplicates = names.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().filter(m -> m.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());

        if (duplicates.size() > 0) {
            throw new ValidationException("Component definition '{}' contains duplicate Stage names: {}",
                    component.getFullName(),
                    String.join(", ", duplicates));
        }
    }
}
