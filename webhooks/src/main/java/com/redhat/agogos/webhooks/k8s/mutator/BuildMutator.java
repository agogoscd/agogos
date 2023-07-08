package com.redhat.agogos.webhooks.k8s.mutator;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class BuildMutator extends Mutator<Build> {

    @Inject
    KubernetesFacade kubernetesFacade;

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

        labels.put(Resource.COMPONENT.getResourceLabel(), componentBuild.getSpec().getComponent());

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
        Component component = kubernetesFacade.get(Component.class, namespace, build.getSpec().getComponent());

        if (component == null) {
            throw new MissingResourceException("Component '{}' does not exist in namespace '{}'",
                    build.getSpec().getComponent(), namespace);
        }

        JsonObject owner = Json.createObjectBuilder()
                .add("apiVersion", HasMetadata.getApiVersion(Component.class))
                .add("kind", HasMetadata.getKind(Component.class))
                .add("name", component.getMetadata().getName())
                .add("uid", component.getMetadata().getUid())
                .add("blockOwnerDeletion", true)
                .add("controller", true)
                .build();

        JsonArray owners = Json.createArrayBuilder()
                .add(owner).build();

        return owners;
    }
}
