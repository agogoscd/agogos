package com.redhat.agogos.k8s.webhooks.validator;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.triggers.ComponentTriggerEvent;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TriggerValidator extends Validator<Trigger> {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerValidator.class);

    @Override
    protected void validateResource(Trigger trigger, AdmissionResponseBuilder responseBuilder) {
        LOG.error("GREG: HERE");
        try {
            trigger.getSpec().getEvents().stream().forEach(event -> {
                if (event instanceof ComponentTriggerEvent) {
                }
            });
            validateTarget(trigger);
            responseBuilder.withAllowed(true);

            LOG.info("Trigger '{}' is valid!", trigger.getFullName());

        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Component Trigger", e);

            responseBuilder.withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(e.getCode())
                            .withMessage(e.getMessage())
                            .build());
        }
    }

    private void validateTarget(Trigger trigger) throws ApplicationException {
        LOG.info("Validating target for trigger '{}'", trigger.getFullName());

        Component target = agogosClient.v1alpha1().components().inNamespace(trigger.getMetadata().getNamespace())
                .withName(trigger.getSpec().getTarget().getName()).get();

        if (target == null) {
            throw new MissingResourceException("Target Component '{}' does not exist in namespace '{}'",
                    trigger.getSpec().getTarget().getName(), trigger.getMetadata().getNamespace());
        }
    }
}
