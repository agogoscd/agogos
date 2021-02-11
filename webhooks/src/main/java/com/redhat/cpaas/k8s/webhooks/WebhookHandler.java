package com.redhat.cpaas.k8s.webhooks;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.cpaas.k8s.webhooks.validator.Validator;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WebhookHandler {
    private static final Logger LOG = Logger.getLogger(WebhookHandler.class);

    @POST
    @Path("validate")
    public AdmissionReview validate(AdmissionReview admissionReview) {

        KubernetesResource object = admissionReview.getRequest().getObject();

        LOG.debugv("New validation request incoming, resource: ''{0}'', requester: ''{1}''",
                object.getClass().getSimpleName(), admissionReview.getRequest().getUserInfo().getUsername());

        return Validator.validatorFor(object).validate(admissionReview);
    }
}
