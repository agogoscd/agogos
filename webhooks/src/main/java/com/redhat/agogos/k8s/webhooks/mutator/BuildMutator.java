
package com.redhat.agogos.k8s.webhooks.mutator;

import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.client.ComponentClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
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
public class BuildMutator extends Mutator<Build> {
    @Inject
    ComponentClient componentClient;

    @Override
    protected void mutateResource(Build build, AdmissionRequest request,
            AdmissionResponseBuilder responseBuilder) {

        String namespace = resourceNamespace(build, request);

        applyPatch(responseBuilder, patchBuilder -> {
            patchBuilder.add("/metadata/labels", generateLabels(build));
            patchBuilder.add("/metadata/ownerReferences", generateOwner(build, namespace));
        });

        responseBuilder.withAllowed(true);
    }

    /**
     * <p>
     * Sets the <code>agogos.redhat.com/component</code> label pointing to the
     * {@link Component}.
     * </p>
     * 
     * @param componentBuild
     * @return Json object containing the label
     */
    private JsonObject generateLabels(Build componentBuild) {
        Map<String, String> labels = componentBuild.getMetadata().getLabels();

        if (labels == null) {
            labels = new HashMap<>();
        }

        labels.put(Resource.COMPONENT.getLabel(), componentBuild.getSpec().getComponent());

        JsonObjectBuilder builder = Json.createObjectBuilder();

        labels.forEach((k, v) -> builder.add(k, v));

        return builder.build();
    }

    /**
     * <p>
     * Sets the Component resource as the owner for to the Component Build.
     * </p>
     * 
     * @param build
     * @return Json array with one entry pointing to the Component
     */
    private JsonArray generateOwner(Build build, String namespace) {
        Component component = componentClient.getByName(build.getSpec().getComponent(),
                namespace);

        if (component == null) {
            throw new MissingResourceException("Selected Component '{}' does not exist in '{}' namespace",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        JsonObject owner = Json.createObjectBuilder() //
                .add("apiVersion", HasMetadata.getApiVersion(Component.class)) //
                .add("kind", HasMetadata.getKind(Component.class)) //
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
