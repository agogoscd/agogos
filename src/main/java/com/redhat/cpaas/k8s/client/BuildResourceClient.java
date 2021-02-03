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
import com.redhat.cpaas.k8s.model.BuildResourceList;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class BuildResourceClient {
    private static final Logger LOG = Logger.getLogger(BuildResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<BuildResource, BuildResourceList, Resource<BuildResource>> buildClient;

    @PostConstruct
    void init() {
        buildClient = kubernetesClient.customResources(BuildResource.class, BuildResourceList.class);
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
