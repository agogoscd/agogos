package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.webhooks.k8s.AdmissionHandler;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RegisterForReflection
public abstract class Validator<T extends CustomResource<?, ?>> extends AdmissionHandler<T> {

    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    @Inject
    KubernetesSerialization objectMapper;

    @Inject
    KubernetesFacade kubernetesFacade;

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
