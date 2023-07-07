package com.redhat.agogos.webhooks.k8s.mutator;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.webhooks.k8s.AdmissionHandler;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.function.Consumer;

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

                response.withAllowed(false)
                        .withStatus(new StatusBuilder()
                                .withCode(e.getCode())
                                .withMessage(e.getMessage())
                                .build());
            }
        });
    }

    /**
     * <p>
     * Returns the namespace for the object which is subject of the
     * {@link AdmissionRequest}.
     * </p>
     * 
     * <p>
     * For some requests the namespace in the object itself may not be set. This
     * applies to object creation requests. We are intercepting the request before
     * the object is created and the namespace is set. In such cases we will use the
     * {@link AdmissionRequest} namespace.
     * </p>
     * 
     * 
     * @param resource
     * @param request
     * @return Namespace
     */
    protected String resourceNamespace(T resource, AdmissionRequest request) {
        if (resource.getMetadata().getNamespace() == null) {
            // Object creation, namespace is not yet set

            if (request.getNamespace() == null) {
                // No namespace set for the request either?
                throw new MissingResourceException("Could not find namespace for new {} request",
                        HasMetadata.getKind(resource.getClass()));
            }

            return request.getNamespace();
        }

        return resource.getMetadata().getNamespace();
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
