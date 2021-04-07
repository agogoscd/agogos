
package com.redhat.cpaas.k8s.webhooks.mutator;

import com.redhat.cpaas.k8s.Resource;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentResource;
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
public class ComponentBuildMutator extends Mutator<ComponentBuildResource> {
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
     * Sets the <code>agogos.redhat.com/component</code> label pointing to the
     * {@link ComponentResource}.
     * </p>
     * 
     * @param componentBuild
     * @return Json object containing the label
     */
    private JsonObject generateLabels(ComponentBuildResource componentBuild) {
        JsonObject labels = Json.createObjectBuilder() //
                .add(Resource.COMPONENT.getLabel(), componentBuild.getSpec().getComponent()) //
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

        if (component == null) {
            throw new MissingResourceException("Selected Component '{}' does not exist in '{}' namespace",
                    componentBuild.getSpec().getComponent(), componentBuild.getMetadata().getNamespace());
        }

        JsonObject owner = Json.createObjectBuilder() //
                .add("apiVersion", HasMetadata.getApiVersion(ComponentResource.class)) //
                .add("kind", HasMetadata.getKind(ComponentResource.class)) //
                .add("name", component.getMetadata().getName()) //
                .add("uid", component.getMetadata().getUid()) //
                .add("blockOwnerDeletion", true) //
                .add("controller", true) //
                .build();

        JsonArray owners = Json.createArrayBuilder() //
                .add(owner).build();

        return owners;
    }
}
