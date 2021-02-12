package com.redhat.cpaas.k8s.webhooks.validator;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
public abstract class Validator<T extends CustomResource<?, ?>> {
    private static final Logger LOG = Logger.getLogger(Validator.class);

    @SuppressWarnings("unchecked")
    public AdmissionReview validate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        LOG.debugv("New validation request incoming, resource: ''{0}'', requester: ''{1}''",
                resource.getClass().getSimpleName(), admissionReview.getRequest().getUserInfo().getUsername());

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
