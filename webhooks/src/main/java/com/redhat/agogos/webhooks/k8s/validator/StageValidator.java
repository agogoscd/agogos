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
    private void validateTaskRef(Stage stage) throws ApplicationException {
        String stageName = stage.getFullName();
        String taskName = stage.getSpec().getTaskRef().getName();

        LOG.info("Validating Stage '{}' Tekton Task reference", stageName);

        if (taskName != null) {
            LOG.debug("Stage '{}' uses a TaskRef with name '{}', checking the existence of that Task", stageName, taskName);
            Task task = kubernetesFacade.get(
                    Task.class,
                    stage.getMetadata().getNamespace(),
                    taskName);

            if (task == null) {
                throw new ApplicationException("Tekton Task '{}' could not be found", taskName);
            }

            LOG.info("Verified that stage '{}' has a valid reference to Tekton Task '{}'", stageName, taskName);
        } else if (stage.getSpec().getTaskRef().getResolver() != null) {
            LOG.info("Stage '{}' uses a TaskRef resolver", stageName);
        } else {
            throw new ApplicationException("Stage '{}' is empty", stageName);
        }
    }
}
