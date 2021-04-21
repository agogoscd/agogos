package com.redhat.agogos.k8s.client;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.BuildResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build CR client.
 * 
 * This bean is used to interact with the {@link ComponentBuildResource} CR.
 * 
 * @author Marek Goldmann
 */
@ApplicationScoped
@RegisterForReflection
public class BuildClient {
    private static final Logger LOG = LoggerFactory.getLogger(BuildClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    MixedOperation<Build, BuildResourceList, Resource<Build>> componentBuildClient;

    @PostConstruct
    void init() {
        componentBuildClient = kubernetesClient.customResources(Build.class, BuildResourceList.class);
    }

    public List<Build> findByLabel(String namespace, String label, String value) {
        return componentBuildClient.inNamespace(namespace).withLabel(label, value).list().getItems();
    }

    public List<Build> findByLabel(String namespace, String label) {
        return componentBuildClient.inNamespace(namespace).withLabel(label).list().getItems();
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
