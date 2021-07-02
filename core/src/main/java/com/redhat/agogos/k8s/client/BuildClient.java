package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.BuildList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;

/**
 * Build CR client.
 * 
 * This bean is used to interact with the {@link ComponentBuildResource} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
@RegisterForReflection
@ToString
public class BuildClient {
    private static final Logger LOG = LoggerFactory.getLogger(BuildClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    MixedOperation<Build, BuildList, Resource<Build>> componentBuildClient;

    @PostConstruct
    void init() {
        componentBuildClient = kubernetesClient.customResources(Build.class, BuildList.class);
    }

    public List<Build> findByLabel(String namespace, String label, String value) {
        return componentBuildClient.inNamespace(namespace).withLabel(label, value).list().getItems();
    }

    public List<Build> findByLabel(String namespace, String label) {
        return componentBuildClient.inNamespace(namespace).withLabel(label).list().getItems();
    }

    public List<Build> list(String namespace) {
        return componentBuildClient.inNamespace(namespace).list().getItems();
    }

    public List<Build> list() {
        return list(kubernetesClient.getNamespace());
    }

    public Build create(String name, String namespace) {

        Build build = new Build();

        build.getMetadata().setGenerateName(name + "-");
        build.getSpec().setComponent(name);

        // TODO: Add exception handling
        return componentBuildClient.inNamespace(namespace).create(build);
    }

    public Build create(Build build, String namespace) {
        // TODO: Add exception handling
        return componentBuildClient.inNamespace(namespace).create(build);
    }
}
