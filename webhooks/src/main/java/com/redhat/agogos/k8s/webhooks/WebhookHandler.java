package com.redhat.agogos.k8s.webhooks;

import com.redhat.agogos.k8s.webhooks.mutator.ComponentBuildMutator;
import com.redhat.agogos.k8s.webhooks.validator.ComponentBuildValidator;
import com.redhat.agogos.k8s.webhooks.validator.ComponentValidator;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.ComponentResource;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class WebhookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookHandler.class);

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

        LOG.debug("New validation request incoming, resource: '{}', requester: '{}'",
                resource.getClass().getSimpleName(), admissionReview.getRequest().getUserInfo().getUsername());

        if (resource instanceof ComponentResource) {
            return componentValidator.validate(admissionReview);
        }

        // If there is no specific handling needed, allow the request
        return AdmissionHandler.allow(admissionReview);
    }

    @POST
    @Path("mutate")
    public AdmissionReview mutate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        LOG.debug("New mutation request incoming, resource: '{}', requester: '{}'", resource.getClass().getSimpleName(),
                admissionReview.getRequest().getUserInfo().getUsername());

        if (resource instanceof Build) {
            return componentBuildMutator.mutate(admissionReview);
        }

        // If there is no specific handling needed, allow the request
        return AdmissionHandler.allow(admissionReview);
    }
}
