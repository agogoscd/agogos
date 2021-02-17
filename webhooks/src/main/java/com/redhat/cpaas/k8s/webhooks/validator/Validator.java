package com.redhat.cpaas.k8s.webhooks.validator;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.k8s.webhooks.AdmissionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
public abstract class Validator<T extends CustomResource<?, ?>> extends AdmissionHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    @SuppressWarnings("unchecked")
    public AdmissionReview validate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        return AdmissionHandler.review(admissionReview, response -> {
            try {
                validateResource((T) resource, response);
            } catch (ApplicationException e) {
                LOG.error("An error occurred while validating", e);

                response.withAllowed(false) //
                        .withStatus(new StatusBuilder() //
                                .withCode(400) //
                                .withMessage(e.getMessage()) //
                                .build());
            }
        });
    }

    protected void validateResource(T resource, AdmissionResponseBuilder responseBuilder) {
        responseBuilder.withAllowed(true);
    }
}
