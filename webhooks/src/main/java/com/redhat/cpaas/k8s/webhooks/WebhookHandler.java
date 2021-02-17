package com.redhat.cpaas.k8s.webhooks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.cpaas.k8s.webhooks.mutator.ComponentBuildMutator;
import com.redhat.cpaas.k8s.webhooks.validator.ComponentBuildValidator;
import com.redhat.cpaas.k8s.webhooks.validator.ComponentValidator;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentResource;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class WebhookHandler {

    @Inject
    ComponentValidator componentValidator;

    @Inject
    ComponentBuildValidator componentBuildValidator;

    @Inject
    ComponentBuildMutator componentBuildMutator;

    @POST
    @Path("validate")
    public AdmissionReview validate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        if (resource instanceof ComponentResource) {
            return componentValidator.validate(admissionReview);
        }

        // By default allow requests
        return allow(admissionReview);
    }

    @POST
    @Path("mutate")
    public AdmissionReview mutate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        if (resource instanceof ComponentBuildResource) {
            return componentBuildMutator.mutate(admissionReview);
        }

        // By default allow requests
        return allow(admissionReview);
    }

    private AdmissionReview allow(AdmissionReview admissionReview) {
        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder() //
                .withUid(admissionReview.getRequest().getUid()).withAllowed(true);

        AdmissionReviewBuilder reviewBuilder = new AdmissionReviewBuilder() //
                .withApiVersion(admissionReview.getApiVersion());

        return reviewBuilder.withResponse(responseBuilder.build()).build();
    }
}
