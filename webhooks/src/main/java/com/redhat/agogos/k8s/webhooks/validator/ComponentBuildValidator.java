package com.redhat.agogos.k8s.webhooks.validator;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.client.ComponentClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.ComponentResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ComponentBuildValidator extends Validator<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentBuildValidator.class);

    @Inject
    ComponentClient componentClient;

    @Override
    protected void validateResource(Build componentBuild, AdmissionResponseBuilder responseBuilder) {
        try {
            validateComponentBuild(componentBuild);
            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Component Build", e);

            responseBuilder.withAllowed(false) //
                    .withStatus(new StatusBuilder() //
                            .withCode(400) //
                            .withMessage(e.getMessage()) //
                            .build());
        }
    }

    private void validateComponentBuild(Build componentBuild) throws ApplicationException {
        LOG.info("Validating component build '{}'", componentBuild.getFullName());

        ComponentResource component = componentClient.getByName(componentBuild.getSpec().getComponent(),
                componentBuild.getMetadata().getNamespace());

        if (component == null) {
            throw new MissingResourceException("Selected Component '{}' does not exist in '{}' namespace",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        LOG.info("Component build '{}' is valid!", componentBuild.getFullName());
    }
}
