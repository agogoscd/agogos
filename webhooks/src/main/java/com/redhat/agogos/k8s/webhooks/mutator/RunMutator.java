
package com.redhat.agogos.k8s.webhooks.mutator;

import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.PipelineClient;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import java.util.MissingResourceException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

@ApplicationScoped
public class RunMutator extends Mutator<Run> {
    @Inject
    PipelineClient pipelineClient;

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
        JsonObject labels = Json.createObjectBuilder() //
                .add(Resource.PIPELINE.getLabel(), run.getSpec().getPipeline()) //
                .build();

        return labels;
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
        Pipeline pipeline = pipelineClient.getByName(run.getSpec().getPipeline(),
                namespace);

        if (pipeline == null) {
            throw new MissingResourceException("Selected Pipeline '{}' does not exist in '{}' namespace",
                    run.getSpec().getPipeline(), namespace);
        }

        JsonObject owner = Json.createObjectBuilder() //
                .add("apiVersion", HasMetadata.getApiVersion(Pipeline.class)) //
                .add("kind", HasMetadata.getKind(Pipeline.class)) //
                .add("name", pipeline.getMetadata().getName()) //
                .add("uid", pipeline.getMetadata().getUid()) //
                .add("blockOwnerDeletion", true) //
                .add("controller", true) //
                .build();

        JsonArray owners = Json.createArrayBuilder() //
                .add(owner).build();

        return owners;
    }
}
