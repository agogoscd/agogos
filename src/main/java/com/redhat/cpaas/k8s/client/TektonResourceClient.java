package com.redhat.cpaas.k8s.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.ApplicationException;
import com.redhat.cpaas.MissingResourceException;
import com.redhat.cpaas.k8s.model.BuildResource;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.model.Builder;
import com.redhat.cpaas.model.Component;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.PipelineBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineList;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRef;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineResult;
import io.fabric8.tekton.pipeline.v1beta1.PipelineResultBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunList;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1beta1.PipelineWorkspaceDeclarationBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskList;
import io.fabric8.tekton.pipeline.v1beta1.TaskRef;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBindingBuilder;

@ApplicationScoped
public class TektonResourceClient {
    public static String CPAAS_SA_NAME = "service";

    @ConfigProperty(name = "kubernetes.storage-class")
    String storageClass;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    @Inject
    BuilderResourceClient builderResourceClient;

    @Inject
    BuildResourceClient buildResourceClient;

    @Inject
    ObjectMapper objectMapper;

    public List<Pipeline> listPipelines() {
        return tektonClient.v1beta1().pipelines().list().getItems();
    }

    public Task getTaskByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        TaskList taskList = tektonClient.v1beta1().tasks().list(options);

        if (taskList.getItems().isEmpty() || taskList.getItems().size() > 1) {
            return null;
        }

        return taskList.getItems().get(0);
    }

    private Pipeline getPipelineByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        PipelineList pipelineList = tektonClient.v1beta1().pipelines().list(options);

        if (pipelineList.getItems().isEmpty() || pipelineList.getItems().size() > 1) {
            return null;
        }

        return pipelineList.getItems().get(0);
    }

    public PipelineRun getPipelineRunByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        PipelineRunList pipelineRunList = tektonClient.v1beta1().pipelineRuns().list(options);

        if (pipelineRunList.getItems().isEmpty() || pipelineRunList.getItems().size() > 1) {
            return null;
        }

        return pipelineRunList.getItems().get(0);
    }

    public List<PipelineRun> listPipelineRuns(String pipelineName) throws MissingResourceException {
        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(String.format("tekton.dev/pipeline=%s", pipelineName)).build();

        return tektonClient.v1beta1().pipelineRuns().list(options).getItems();
    }

    public PipelineRun runPipeline(String componentName, String buildName) throws ApplicationException {
        Pipeline pipeline = this.getPipelineByName(componentName);

        if (pipeline == null) {
            throw new MissingResourceException(String.format("Pipeline '%s' not found in the system", componentName));
        }

        PipelineRef pipelineRef = new PipelineRefBuilder(true).withName(pipeline.getMetadata().getName()).build();

        Map<String, Quantity> requests = new HashMap<String, Quantity>();
        requests.put("storage", new Quantity("1Gi"));

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder() //
                .withNewSpec() //
                .withNewResources().withRequests(requests).endResources() //
                .withStorageClassName(storageClass) //
                .withAccessModes("ReadWriteOnce") //
                .endSpec()//
                .build();

        WorkspaceBinding workspaceBinding = new WorkspaceBindingBuilder() //
                .withName("ws") //
                .withVolumeClaimTemplate(pvc) //
                .build();

        BuildResource build = buildResourceClient.getByName(buildName);

        if (build == null) {
            throw new MissingResourceException(String.format("Selected build '%s' does not exist", buildName));
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("cpaas.redhat.com/build", buildName);

        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(build.getApiVersion()) //
                .withKind(build.getKind()) //
                .withName(build.getMetadata().getName()) //
                .withUid(build.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        PipelineRun pipelineRun = new PipelineRunBuilder() //
                .withNewMetadata() //
                .withOwnerReferences(ownerReference) //
                .withName(buildName) //
                .withLabels(labels) //
                .endMetadata() //
                .withNewSpec() //
                .withServiceAccountName(CPAAS_SA_NAME) //
                .withPipelineRef(pipelineRef) //
                .withWorkspaces(workspaceBinding) //
                .endSpec() //
                .build();

        return tektonClient.v1beta1().pipelineRuns().inNamespace(build.getMetadata().getNamespace())
                .create(pipelineRun);
    }

    public Pipeline createPipeline(ComponentResource component) throws ApplicationException {
        // Find the builder defined by the component
        Builder builder = builderResourceClient.getByName(component.getSpec().getBuilder());

        if (builder == null) {
            throw new MissingResourceException(String.format("Selected builder '%s' is not registered in the system",
                    component.getSpec().getBuilder()));
        }

        String componentJson;

        // Convert Component metadata to JSON
        try {
            componentJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Component(component));
        } catch (JsonProcessingException e) {
            throw new ApplicationException(String.format("Internal error; could not serialize component '%s'",
                    component.getMetadata().getName()), e);
        }

        List<PipelineTask> tasks = new ArrayList<>();

        // Prepare workspace for main task to store results
        WorkspacePipelineTaskBinding resultsWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("results") //
                .withWorkspace("ws") //
                .withSubPath("results") //
                .build();

        // Prepare workspace for main task to share content between steps
        WorkspacePipelineTaskBinding sharedWsBinding = new WorkspacePipelineTaskBindingBuilder() //
                .withName("shared") //
                .withWorkspace("ws") //
                .withSubPath("shared") //
                .build();

        // TODO: Make pre and post-tasks configurable?
        // Prepare main task
        PipelineTask buildTask = new PipelineTaskBuilder() //
                .withName(component.getMetadata().getName()) //
                .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", component.getSpec().getBuilder())) //
                .addNewParam() //
                .withName("component") //
                .withNewValue(componentJson) //
                .endParam() //
                .withWorkspaces(resultsWsBinding, sharedWsBinding) //
                .build();

        tasks.add(buildTask);

        // // TODO: Remove, only for now
        // PipelineTask s3Task = new PipelineTaskBuilder() //
        // .withName("s3") //
        // .withTaskRef(new TaskRef("tekton.dev/v1beta1", "Task", "s3")) //
        // .addNewParam() //
        // .withName("component") //
        // .withNewValue(componentJson) //
        // .endParam() //
        // .withWorkspaces(resultsWsBinding, sharedWsBinding) //
        // .withRunAfter(component.getMetadata().getName()) // TODO: hardcoded
        // .build();

        // tasks.add(s3Task);

        // Define main workspace
        PipelineWorkspaceDeclaration workspaceMain = new PipelineWorkspaceDeclarationBuilder() //
                .withName("ws") //
                .withDescription("Main workspace") //
                .build();

        // Pipeline result is the result of the main task executed
        PipelineResult pipelineResult = new PipelineResultBuilder() //
                .withName("data") //
                .withValue("$(tasks.".concat(component.getMetadata().getName()).concat(".results.data)")) //
                .build();

        // Add any useful/required labels
        Map<String, String> labels = new HashMap<>();
        labels.put("cpaas.redhat.com/component", component.getMetadata().getName());

        // Make sure the Pipeline is owned by the Component
        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(component.getApiVersion()) //
                .withKind(component.getKind()) //
                .withName(component.getMetadata().getName()) //
                .withUid(component.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        // Define the Pipeline itself
        Pipeline pipeline = new PipelineBuilder() //
                .withNewMetadata() //
                .withOwnerReferences(ownerReference) //
                .withLabels(labels) //
                .withName(component.getMetadata().getName()) //
                .endMetadata() //
                .withNewSpec() //
                .withWorkspaces(workspaceMain) //
                .addAllToTasks(tasks) //
                .withResults(pipelineResult) //
                .endSpec() //
                .build();

        return tektonClient.v1beta1().pipelines().inNamespace(component.getMetadata().getNamespace())
                .createOrReplace(pipeline);
    }
}
