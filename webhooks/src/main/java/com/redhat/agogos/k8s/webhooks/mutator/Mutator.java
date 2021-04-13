package com.redhat.agogos.k8s.webhooks.mutator;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.webhooks.AdmissionHandler;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Base64;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RegisterForReflection
public abstract class Mutator<T extends CustomResource<?, ?>> extends AdmissionHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Mutator.class);

    /**
     * Main method that is called to request mutation on an object received with the
     * {@link AdmissionReview}.
     * 
     * @param admissionReview Incoming {@link AdmissionReview} object
     * @return An admission review object returned by the webhook
     */
    @SuppressWarnings("unchecked")
    public AdmissionReview mutate(AdmissionReview admissionReview) {
        AdmissionRequest request = admissionReview.getRequest();
        KubernetesResource resource = request.getObject();

        return AdmissionHandler.review(admissionReview, response -> {
            try {
                mutateResource((T) resource, request, response);
            } catch (ApplicationException e) {
                LOG.error("An error occurred while mutating", e);

                response.withAllowed(false) //
                        .withStatus(new StatusBuilder() //
                                .withCode(400) //
                                .withMessage(e.getMessage()) //
                                .build());
            }
        });
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
