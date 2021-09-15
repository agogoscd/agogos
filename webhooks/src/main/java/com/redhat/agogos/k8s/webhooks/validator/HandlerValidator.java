package com.redhat.agogos.k8s.webhooks.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Handler;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class HandlerValidator extends Validator<Handler> {

    private static final Logger LOG = LoggerFactory.getLogger(HandlerValidator.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AgogosClient agogosClient;

    @Inject
    TektonClient tektonClient;

    @Override
    protected void validateResource(Handler resource, AdmissionResponseBuilder responseBuilder) {
        try {
            Task task = validateTaskRef(resource);

            validateWorkspaces(task, resource);

            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Handler", e);

            responseBuilder.withAllowed(false) //
                    .withStatus(new StatusBuilder() //
                            .withCode(400) //
                            .withMessage(e.getMessage()) //
                            .build());
        }
    }

    /**
     * <p>
     * Validates workspace configuration of {@link Handler}.
     * </p>
     */
    private void validateWorkspaces(Task task, Handler handler) throws ApplicationException {
        LOG.info("Validating Handler '{}' workspace configuration", handler.getFullName());

        // Workspaces declared in Tekton Task
        List<String> taskWorkspaces = task.getSpec().getWorkspaces().stream()
                .map(w -> w.getName())
                .collect(Collectors.toList());

        // Workspaces declared in Handler's mapping
        List<String> declaredWorkspaces = handler.getSpec().getWorkspaces().stream().map(w -> w.getName())
                .collect(Collectors.toList());

        // Workspaces that are declared in the workspace mapping but are not declared on the Tekton Task
        List<String> excessWorkspaces = declaredWorkspaces.stream().filter(w -> !taskWorkspaces.contains(w))
                .collect(Collectors.toList());

        if (excessWorkspaces.size() > 0) {
            throw new ApplicationException(
                    "Defined workspace mapping in Handler '{}' is invalid: workspace mapping contains workspaces non-existing in the Tekton Task: {}",
                    handler.getMetadata().getName(), Arrays.toString(excessWorkspaces.toArray()));
        }

        // Workspaces declared in Tekton Task and marked as required
        List<String> requiredWorkspaces = task.getSpec().getWorkspaces().stream()
                .filter(w -> w.getOptional() == null || w.getOptional() == false)
                .map(w -> w.getName())
                .collect(Collectors.toList());

        // Workspaces that are required but not provided by the mapping
        List<String> missedWorkspaces = requiredWorkspaces.stream().filter(w -> !declaredWorkspaces.contains(w))
                .collect(Collectors.toList());

        if (missedWorkspaces.size() > 0) {
            throw new ApplicationException(
                    "Defined workspace mapping in Handler '{}' is invalid: workspace mapping is missing following required workspaces: {}",
                    handler.getMetadata().getName(), missedWorkspaces);
        }

        LOG.info("Handler '{}' has a valid workspace configuration", handler.getFullName());
    }

    /**
     * <p>
     * Validates {@link Task} reference correctness.
     * </p>
     */
    private Task validateTaskRef(Handler handler) throws ApplicationException {
        LOG.info("Validating Handler '{}' Tekton Task reference", handler.getFullName());

        Task task = tektonClient.v1beta1().tasks().inNamespace(handler.getMetadata().getNamespace())
                .withName(handler.getSpec().getTaskRef().getName()).get();

        if (task == null) {
            throw new ApplicationException("Tekton Task '{}' could not be found", handler.getSpec().getTaskRef().getName());
        }

        LOG.info("Handler '{}' has a valid Tekton Task reference", handler.getFullName());

        return task;
    }
}
