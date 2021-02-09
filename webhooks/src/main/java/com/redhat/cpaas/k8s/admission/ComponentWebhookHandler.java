package com.redhat.cpaas.k8s.admission;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.client.StageResourceClient;
import com.redhat.cpaas.k8s.errors.ApplicationException;
import com.redhat.cpaas.k8s.errors.MissingResourceException;
import com.redhat.cpaas.k8s.errors.ValidationException;
import com.redhat.cpaas.k8s.model.AbstractStage.Phase;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.StageResource;

import org.jboss.logging.Logger;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;

@Path("/webhooks/admission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComponentWebhookHandler {
    private static final Logger LOG = Logger.getLogger(ComponentWebhookHandler.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    StageResourceClient stageResourceClient;

    @POST
    public AdmissionReview validate(AdmissionReview reviewRequest) {
        KubernetesResource object = reviewRequest.getRequest().getObject();

        LOG.debugv("New validation request incoming, resource: ''{0}'', requester: ''{1}''",
                object.getClass().getSimpleName(), reviewRequest.getRequest().getUserInfo().getUsername());

        LOG.tracev("Resource to validate: {0}", object);

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder() //
                .withUid(reviewRequest.getRequest().getUid());

        AdmissionReviewBuilder reviewBuilder = new AdmissionReviewBuilder() //
                .withApiVersion(reviewRequest.getApiVersion());

        if (!(object instanceof ComponentResource)) {
            responseBuilder.withAllowed(false) //
                    .withStatus(new StatusBuilder() //
                            .withCode(400) //
                            .withMessage("Unsupported resource type: " + object.getClass().getSimpleName()) //
                            .build());

            return reviewBuilder.withResponse(responseBuilder.build()).build();
        }

        if (object instanceof ComponentResource) {
            try {
                validateComponent((ComponentResource) object);

                responseBuilder.withAllowed(true);
            } catch (ApplicationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                responseBuilder.withAllowed(false) //
                        .withStatus(new StatusBuilder() //
                                .withCode(400) //
                                .withMessage(e.getMessage()) //
                                .build());
            }
        }

        return reviewBuilder.withResponse(responseBuilder.build()).build();
    }

    private void validateComponent(ComponentResource component) throws ApplicationException {
        StageResource builder = stageResourceClient.getByName(component.getSpec().getBuilder(), Phase.BUILD);

        if (builder == null) {
            throw new MissingResourceException(String.format("Selected builder '%s' is not registered in the system",
                    component.getSpec().getBuilder()));
        }

        LOG.infov("Validating component ''{0}''", component.getMetadata().getName());

        ValidationData<Void> validation = new ValidationData<>();

        JsonNode schemaNode = objectMapper.valueToTree(builder.getSpec().getSchema().getOpenAPIV3Schema());
        JsonNode contentNode = objectMapper.valueToTree(component.getSpec().getData());

        LOG.debugv("Validating component ''{0}'' content: ''{1}'' with schema: ''{2}''",
                component.getMetadata().getName(), contentNode, schemaNode);

        SchemaValidator schemaValidator;

        try {
            schemaValidator = new SchemaValidator(null, schemaNode);
        } catch (ResolutionException e) {
            e.printStackTrace();
            throw new ApplicationException("Could not instantiate validator", e);
        }

        schemaValidator.validate(contentNode, validation);

        if (!validation.isValid()) {
            List<String> errorMessages = validation.results().items().stream()
                    .map(item -> item.message().replaceAll("\\.+$", "")).collect(Collectors.toList());

            errorMessages.forEach(message -> {
                LOG.errorv("Validation error for ''{0}'' component: ''{1}''", component.getMetadata().getName(),
                        message);
            });

            throw new ValidationException(
                    "Component definition '" + component.getMetadata().getName() + "' is not valid", errorMessages);
        }

        LOG.infov("Component ''{0}'' is valid!", component.getMetadata().getName());
    }

}
