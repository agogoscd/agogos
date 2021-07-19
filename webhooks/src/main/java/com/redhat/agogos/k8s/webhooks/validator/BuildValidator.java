package com.redhat.agogos.k8s.webhooks.validator;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BuildValidator extends Validator<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildValidator.class);

    @Inject
    AgogosClient agogosClient;

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

        Component component = agogosClient.v1alpha1().components().inNamespace(componentBuild.getMetadata().getNamespace())
                .withName(componentBuild.getSpec().getComponent()).get();

        if (component == null) {
            throw new MissingResourceException("Selected Component '{}' does not exist in '{}' namespace",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        LOG.info("Component build '{}' is valid!", componentBuild.getFullName());
    }
}