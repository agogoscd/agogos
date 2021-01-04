package com.redhat.cpaas.k8s.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.MissingResourceException;
import com.redhat.cpaas.k8s.model.BuildResource;
import com.redhat.cpaas.k8s.model.BuildResource.BuildStatus;
import com.redhat.cpaas.k8s.model.BuildResource.Status;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

@ApplicationScoped
public class BuildResourceClient {
    private static final Logger LOG = Logger.getLogger(BuildResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    NonNamespaceOperation<BuildResource, BuildResourceList, BuildResourceDoneable, Resource<BuildResource, BuildResourceDoneable>> buildClient;

    @PostConstruct
    void init() {
        KubernetesDeserializer.registerCustomKind("cpaas.redhat.com/v1alpha1", BuildResource.KIND, BuildResource.class);

        final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                .withName("builds.cpaas.redhat.com") //
                .withGroup("cpaas.redhat.com") //
                .withScope("Namespaced") //
                .withVersion("v1alpha1") //
                .withPlural("builds") //
                .build();

        buildClient = kubernetesClient
                .customResources(context, BuildResource.class, BuildResourceList.class, BuildResourceDoneable.class);
    }

    public BuildResource updateStatus(final BuildResource build, Status status, String reason) {
        BuildStatus buildStatus = build.getStatus();
        buildStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        buildStatus.setStatus(String.valueOf(status));
        buildStatus.setReason(reason);

        // Update the Status sub-resource, this will not trigger another reconcile.
        return buildClient.updateStatus(build);
    }

    public BuildResource getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        BuildResourceList buildResources = buildClient.list(options);

        if (buildResources.getItems().isEmpty() || buildResources.getItems().size() > 1) {
            return null;
        }

        return buildResources.getItems().get(0);
    }

    public BuildResource create(String componentName) {
        Map<String, String> labels = new HashMap<>();

        labels.put("cpaas.redhat.com/component", componentName);

        BuildResource build = new BuildResource();
        build.getMetadata().setGenerateName(componentName + "-");
        build.getMetadata().setLabels(labels);
        build.getSpec().setComponent(componentName);
        return buildClient.create(build);
    }

    public List<BuildResource> listBuilds(String componentName) throws MissingResourceException {
        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(String.format("cpaas.redhat.com/component=%s", componentName)).build();
        // .withLabelSelector(String.format("tekton.dev/pipeline=%s", pipelineName))
        return buildClient.list(options).getItems();
    }
}
