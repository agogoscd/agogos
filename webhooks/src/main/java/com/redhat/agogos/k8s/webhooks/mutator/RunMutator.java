package com.redhat.agogos.k8s.webhooks.mutator;

import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class RunMutator extends Mutator<Run> {
    @Inject
    AgogosClient agogosClient;

    @Override
    protected void mutateResource(Run run, AdmissionRequest request,
            AdmissionResponseBuilder responseBuilder) {

        String namespace = resourceNamespace(run, request);

        applyPatch(responseBuilder, patchBuilder -> {
            patchBuilder.add("/metadata/labels", generateLabels(run));
            patchBuilder.add("/metadata/ownerReferences", generateOwner(run, namespace));
        });

        responseBuilder.withAllowed(true);
    }

    /**
     * <p>
     * Sets the <code>agogos.redhat.com/pipeline</code> label pointing to the
     * {@link Pipeline}.
     * </p>
     * 
     * @param Run run
     * @return Json object containing the label
     */
    private JsonObject generateLabels(Run run) {
        Map<String, String> labels = run.getMetadata().getLabels();

        if (labels == null) {
            labels = new HashMap<>();
        }

        labels.put(Resource.PIPELINE.getLabel(), run.getSpec().getPipeline());

        JsonObjectBuilder builder = Json.createObjectBuilder();

        labels.forEach((k, v) -> builder.add(k, v));

        return builder.build();
    }

    /**
     * <p>
     * Sets the Pipeline resource as the owner for to the Run.
     * </p>
     * 
     * @param Run run
     * @return Json array with one entry with Pipeline
     */
    private JsonArray generateOwner(Run run, String namespace) {
        Pipeline pipeline = agogosClient.v1alpha1().pipelines().inNamespace(namespace).withName(run.getSpec().getPipeline())
                .get();

        if (pipeline == null) {
            throw new MissingResourceException(
                    "Pipeline '{}' does not exist in namespace '{}'",
                    run.getSpec().getPipeline(), namespace);
        }

        JsonObject owner = Json.createObjectBuilder()
                .add("apiVersion", HasMetadata.getApiVersion(Pipeline.class))
                .add("kind", HasMetadata.getKind(Pipeline.class))
                .add("name", pipeline.getMetadata().getName())
                .add("uid", pipeline.getMetadata().getUid())
                .add("blockOwnerDeletion", true)
                .add("controller", true)
                .build();

        JsonArray owners = Json.createArrayBuilder()
                .add(owner).build();

        return owners;
    }
}
