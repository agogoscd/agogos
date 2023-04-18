package com.redhat.agogos.k8s.webhooks.validator;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.webhooks.AdmissionHandler;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

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
                                .withCode(e.getCode()) //
                                .withMessage(e.getMessage()) //
                                .build());
            }
        });
    }

    protected void validateResource(T resource, AdmissionResponseBuilder responseBuilder) {
        responseBuilder.withAllowed(true);
    }
}
