package com.redhat.agogos.k8s.webhooks;

import com.redhat.agogos.k8s.webhooks.mutator.BuildMutator;
import com.redhat.agogos.k8s.webhooks.mutator.RunMutator;
import com.redhat.agogos.k8s.webhooks.validator.ComponentBuildValidator;
import com.redhat.agogos.k8s.webhooks.validator.ComponentValidator;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Run;
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
    ComponentBuildValidator buildValidator;

    @Inject
    BuildMutator buildMutator;

    @Inject
    RunMutator runMutator;

    @POST
    @Path("validate")
    public AdmissionReview validate(AdmissionReview admissionReview) {
        KubernetesResource resource = admissionReview.getRequest().getObject();

        LOG.debug("New validation request incoming, resource: '{}', requester: '{}'",
                resource.getClass().getSimpleName(), admissionReview.getRequest().getUserInfo().getUsername());

        if (resource instanceof Component) {
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
            return buildMutator.mutate(admissionReview);
        }

        if (resource instanceof Run) {
            return runMutator.mutate(admissionReview);
        }

        // If there is no specific handling needed, allow the request
        return AdmissionHandler.allow(admissionReview);
    }
}
