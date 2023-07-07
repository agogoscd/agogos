package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.triggers.ComponentTriggerEvent;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import com.redhat.agogos.core.v1alpha1.triggers.TriggerTarget;
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

        TriggerTarget target = trigger.getSpec().getTarget();
        AgogosResource<?, ?> resource = null;
        switch (Resource.fromType(target.getKind())) {
            case COMPONENT:
                resource = kubernetesFacade.get(Component.class, trigger.getMetadata().getNamespace(), target.getName());
                break;
            case GROUP:
                resource = kubernetesFacade.get(Group.class, trigger.getMetadata().getNamespace(), target.getName());
                break;
            case PIPELINE:
                resource = kubernetesFacade.get(Pipeline.class, trigger.getMetadata().getNamespace(), target.getName());
                break;
            default:
                // Fall through
        }

        if (resource == null) {
            throw new MissingResourceException("Target {} '{}' does not exist in namespace '{}'",
                    target.getKind(), target.getName(), trigger.getMetadata().getNamespace());
        }
    }
}
