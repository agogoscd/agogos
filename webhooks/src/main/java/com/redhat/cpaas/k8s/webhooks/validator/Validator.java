package com.redhat.cpaas.k8s.webhooks.validator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import com.redhat.cpaas.k8s.model.ComponentResource;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;

@ApplicationScoped
public class Validator<T extends KubernetesResource> {
    private static final Logger LOG = Logger.getLogger(Validator.class);

    @SuppressWarnings("rawtypes")
    public static Validator validatorFor(KubernetesResource resource) {
        if (resource instanceof ComponentResource) {
            return CDI.current().select(ComponentValidator.class).get();
        }

        return new Validator();
    }

    @SuppressWarnings("unchecked")
    public AdmissionReview validate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder() //
                .withUid(admissionReview.getRequest().getUid());

        AdmissionReviewBuilder reviewBuilder = new AdmissionReviewBuilder() //
                .withApiVersion(admissionReview.getApiVersion());

        validateResource((T) resource, responseBuilder);

        return reviewBuilder.withResponse(responseBuilder.build()).build();
    }

    protected void validateResource(T resource, AdmissionResponseBuilder responseBuilder) {
        responseBuilder.withAllowed(true);
    }
}
