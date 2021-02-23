package com.redhat.cpaas.k8s.webhooks.validator;

import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ComponentBuildValidator extends Validator<ComponentBuildResource> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentBuildValidator.class);

    @Inject
    ComponentResourceClient componentResourceClient;

    @Override
    protected void validateResource(ComponentBuildResource componentBuild, AdmissionResponseBuilder responseBuilder) {
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

    private void validateComponentBuild(ComponentBuildResource componentBuild) throws ApplicationException {
        LOG.info("Validating component build '{}'", componentBuild.getNamespacedName());

        ComponentResource component = componentResourceClient.getByName(componentBuild.getSpec().getComponent());

        if (component == null) {
            throw new MissingResourceException("Selected Component '{}' does not exist in '{}' namespace",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        LOG.info("Component build '{}' is valid!", componentBuild.getNamespacedName());
    }
}
