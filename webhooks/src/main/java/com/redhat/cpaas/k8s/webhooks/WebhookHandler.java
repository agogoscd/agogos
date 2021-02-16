package com.redhat.cpaas.k8s.webhooks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.cpaas.k8s.webhooks.mutator.ComponentBuildMutator;
import com.redhat.cpaas.k8s.webhooks.validator.ComponentValidator;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class WebhookHandler {

    @Inject
    ComponentValidator componentValidator;

    @Inject
    ComponentBuildMutator componentBuildMutator;

    @POST
    @Path("validate/component")
    public AdmissionReview validateComponent(AdmissionReview admissionReview) {
        return componentValidator.validate(admissionReview);
    }

    @POST
    @Path("mutate/componentbuild")
    public AdmissionReview mutateComponentBuild(AdmissionReview admissionReview) {
        return componentBuildMutator.mutate(admissionReview);
    }
}
