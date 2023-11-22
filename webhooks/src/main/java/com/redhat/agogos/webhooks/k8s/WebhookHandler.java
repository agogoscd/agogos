package com.redhat.agogos.webhooks.k8s;

import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import com.redhat.agogos.webhooks.k8s.mutator.BuildMutator;
import com.redhat.agogos.webhooks.k8s.mutator.RunMutator;
import com.redhat.agogos.webhooks.k8s.validator.BuildValidator;
import com.redhat.agogos.webhooks.k8s.validator.ComponentValidator;
import com.redhat.agogos.webhooks.k8s.validator.StageValidator;
import com.redhat.agogos.webhooks.k8s.validator.TriggerValidator;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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
    StageValidator StageValidator;

    @Inject
    TriggerValidator triggerValidator;

    @Inject
    BuildValidator buildValidator;

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

        if (resource instanceof Stage) {
            return StageValidator.validate(admissionReview);
        }

        if (resource instanceof Trigger) {
            return triggerValidator.validate(admissionReview);
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
