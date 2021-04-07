package com.redhat.cpaas.k8s;

import com.redhat.cpaas.errors.MissingResourceException;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import com.redhat.cpaas.v1alpha1.PipelineResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineList;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@RegisterForReflection
public class TektonPipelineHelper {

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @ConfigProperty(name = "kubernetes.storage-class")
    Optional<String> storageClass;

    @Inject
    TektonClient tektonClient;

    private Pipeline getPipelineByName(String name, String namespace) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        PipelineList pipelineList = tektonClient.v1beta1().pipelines().inNamespace(namespace).list(options);

        if (pipelineList.getItems().isEmpty() || pipelineList.getItems().size() > 1) {
            return null;
        }

        return pipelineList.getItems().get(0);
    }

    // TODO: temporar
    public PipelineRun run(PipelineRun pipelineRun, String namespace) {
        return tektonClient.v1beta1().pipelineRuns().inNamespace(namespace).create(pipelineRun);
    }

    public PipelineRun run(String kind, String name, String namespace, HasMetadata owner) {
        PipelineRun pipelineRun = generate(kind, name, namespace, owner);

        return tektonClient.v1beta1().pipelineRuns().inNamespace(namespace).create(pipelineRun);
    }

    public PipelineRun run(String kind, String name, String namespace) {
        return run(kind, name, namespace, null);
    }

    public PipelineRun generate(String kind, String name, String namespace) {
        return generate(kind, name, namespace, null);
    }

    /**
     * Prepares a {@link PipelineRun} resource to trigger a new Tekton pipeline
     * responsible for running a pipeline defined by the resource type.
     * 
     * For a {@link ComponentResource} a new build will be created. For a
     * {@link PipelineResource} a new run will be created.
     * 
     * @param component
     * @return The {@link PipelineRun} resource to be created
     */
    public PipelineRun generate(String kind, String name, String namespace, HasMetadata owner) {
        Pipeline pipeline = getPipelineByName(name, namespace);

        if (pipeline == null) {
            throw new MissingResourceException("Pipeline '{}' not found in the '{}' namespace", name, namespace);
        }

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("storage", Quantity.parse("1Gi"));

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder() //
                .withNewSpec() //
                .withAccessModes("ReadWriteOnce") //
                .withNewResources().withRequests(requests).endResources() //
                .withStorageClassName(storageClass.orElse("")) //
                .endSpec() //
                .build();

        WorkspaceBinding workspace = new WorkspaceBindingBuilder() //
                .withName("ws") //
                .withVolumeClaimTemplate(pvc) //
                .build();

        Map<String, String> labels = new HashMap<>();
        labels.put(Resource.RESOURCE.getLabel(), kind.toLowerCase());

        PipelineRun pipelineRun = new PipelineRunBuilder() //
                .withNewMetadata() //
                .withLabels(labels) //
                .withGenerateName(name + "-") //
                .withNamespace(namespace) //
                .endMetadata() //
                .withNewSpec() //
                .withNewPipelineRef().withName(name).endPipelineRef() //
                .withWorkspaces(workspace) //
                .endSpec() //
                .build();

        if (owner != null) {
            OwnerReference ownerReference = new OwnerReferenceBuilder() //
                    .withApiVersion(owner.getApiVersion()) //
                    .withKind(owner.getKind()) //
                    .withName(owner.getMetadata().getName()) //
                    .withUid(owner.getMetadata().getUid()) //
                    .withBlockOwnerDeletion(true) //
                    .withController(true) //
                    .build();

            pipelineRun.getMetadata().setOwnerReferences(Arrays.asList(ownerReference));
        }

        if (serviceAccount.isPresent()) {
            pipelineRun.getSpec().setServiceAccountName(serviceAccount.get());
        }

        return pipelineRun;
    }
}
