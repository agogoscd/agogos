package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.Builder;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuilderValidator extends Validator<Builder> {

    private static final Logger LOG = LoggerFactory.getLogger(BuilderValidator.class);

    @Override
    protected void validateResource(Builder builder, AdmissionResponseBuilder responseBuilder) {
        try {
            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Builder", e);

            responseBuilder.withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(e.getCode())
                            .withMessage(e.getMessage())
                            .build());
        }
    }

}
