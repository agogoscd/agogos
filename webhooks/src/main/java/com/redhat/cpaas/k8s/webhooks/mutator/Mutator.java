package com.redhat.cpaas.k8s.webhooks.mutator;

import java.util.Base64;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;

import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.k8s.webhooks.AdmissionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@ApplicationScoped
@RegisterForReflection
public abstract class Mutator<T extends CustomResource<?, ?>> extends AdmissionHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Mutator.class);

    @SuppressWarnings("unchecked")
    public AdmissionReview mutate(AdmissionReview admissionReview) {
        AdmissionRequest request = admissionReview.getRequest();
        KubernetesResource resource = request.getObject();

        LOG.debug("New mutation request incoming, resource: '{}', requester: '{}'", resource.getClass().getSimpleName(),
                request.getUserInfo().getUsername());

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder() //
                .withUid(request.getUid());

        AdmissionReviewBuilder reviewBuilder = new AdmissionReviewBuilder() //
                .withApiVersion(admissionReview.getApiVersion());

        try {
            mutateResource((T) resource, request, responseBuilder);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while mutating", e);

            responseBuilder.withAllowed(false) //
                    .withStatus(new StatusBuilder() //
                            .withCode(400) //
                            .withMessage(e.getMessage()) //
                            .build());
        }

        return reviewBuilder.withResponse(responseBuilder.build()).build();
    }

    protected void mutateResource(T resource, AdmissionRequest request, AdmissionResponseBuilder responseBuilder) {
        responseBuilder.withAllowed(true);
    }

    /**
     * Automates patching of incoming resources.
     * 
     * @param responseBuilder The response that should be added
     * @param builder
     */
    public void applyPatch(AdmissionResponseBuilder responseBuilder, Consumer<JsonPatchBuilder> consumer) {
        JsonPatchBuilder patchBuilder = Json.createPatchBuilder();

        consumer.accept(patchBuilder);

        JsonPatch jsonPatch = patchBuilder.build();

        LOG.trace(jsonPatch.toString());

        responseBuilder.withPatch(Base64.getEncoder().encodeToString(jsonPatch.toString().getBytes()))
                .withPatchType("JSONPatch");

    }

}
