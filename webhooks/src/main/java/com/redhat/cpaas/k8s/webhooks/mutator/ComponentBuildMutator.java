
package com.redhat.cpaas.k8s.webhooks.mutator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentResource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;

@ApplicationScoped
public class ComponentBuildMutator extends Mutator<ComponentBuildResource> {
    private static final String COMPONENT_LABEL = "cpaas.redhat.com/component";

    @Inject
    ComponentResourceClient componentResourceClient;

    @Override
    protected void mutateResource(ComponentBuildResource componentBuild, AdmissionRequest request,
            AdmissionResponseBuilder responseBuilder) {

        applyPatch(responseBuilder, patchBuilder -> {
            patchBuilder.add("/metadata/labels", generateLabels(componentBuild));
            patchBuilder.add("/metadata/ownerReferences", generateOwner(componentBuild));
        });

        responseBuilder.withAllowed(true);
    }

    /**
     * <p>
     * Sets the <code>cpaas.redhat.com/component</code> label pointing to the
     * {@link ComponentResource}.
     * </p>
     * 
     * @param componentBuild
     * @return Json object containing the label
     */
    private JsonObject generateLabels(ComponentBuildResource componentBuild) {
        JsonObject labels = Json.createObjectBuilder() //
                .add(COMPONENT_LABEL, componentBuild.getSpec().getComponent()) //
                .build();

        return labels;
    }

    /**
     * <p>
     * Sets the Component resource as the owner for to the Component Build.
     * </p>
     * 
     * @param componentBuild
     * @return Json array with one entry pointing to the Component
     */
    private JsonArray generateOwner(ComponentBuildResource componentBuild) {
        ComponentResource component = componentResourceClient.getByName(componentBuild.getSpec().getComponent());

        JsonObject owner = Json.createObjectBuilder() //
                .add("apiVersion", HasMetadata.getApiVersion(ComponentResource.class)) //
                .add("kind", HasMetadata.getKind(ComponentResource.class)) //
                .add("name", component.getMetadata().getName()) //
                .add("uid", component.getMetadata().getUid()) //
                .add("blockOwnerDeletion", true) //
                .build();

        JsonArray owners = Json.createArrayBuilder() //
                .add(owner).build();

        return owners;
    }
}
