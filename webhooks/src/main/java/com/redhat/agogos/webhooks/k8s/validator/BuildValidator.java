package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BuildValidator extends Validator<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildValidator.class);

    @Override
    protected void validateResource(Build componentBuild, AdmissionResponseBuilder responseBuilder) {
        try {
            validateComponentBuild(componentBuild);
            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Component Build", e);

            responseBuilder.withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(e.getCode())
                            .withMessage(e.getMessage())
                            .build());
        }
    }

    private void validateComponentBuild(Build componentBuild) throws ApplicationException {
        LOG.info("Validating component build '{}'", componentBuild.getFullName());

        Component component = kubernetesFacade.get(
                Component.class,
                componentBuild.getMetadata().getNamespace(),
                componentBuild.getSpec().getComponent());

        if (component == null) {
            throw new MissingResourceException("Component '{}' does not exist in namespace '{}'",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        LOG.info("Component build '{}' is valid!", componentBuild.getFullName());
    }
}
