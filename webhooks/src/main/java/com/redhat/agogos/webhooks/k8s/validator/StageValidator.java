package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.Stage;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StageValidator extends Validator<Stage> {

    private static final Logger LOG = LoggerFactory.getLogger(StageValidator.class);

    @Override
    protected void validateResource(Stage resource, AdmissionResponseBuilder responseBuilder) {
        try {
            validateTaskRef(resource);

            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Stage '{}'", resource.getFullName(), e);

            responseBuilder.withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(e.getCode())
                            .withMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * <p>
     * Validates {@link Task} reference correctness.
     * </p>
     */
    private Task validateTaskRef(Stage stage) throws ApplicationException {
        LOG.info("Validating Stage '{}' Tekton Task reference", stage.getFullName());

        Task task = kubernetesFacade.get(Task.class, stage.getMetadata().getNamespace(),
                stage.getSpec().getTaskRef().getName());
        if (task == null) {
            throw new ApplicationException("Tekton Task '{}' could not be found", stage.getSpec().getTaskRef().getName());
        }

        LOG.info("Stage '{}' has a valid Tekton Task reference", stage.getFullName());

        return task;
    }
}
