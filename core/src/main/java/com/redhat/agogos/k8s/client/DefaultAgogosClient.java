package com.redhat.agogos.k8s.client;

import io.fabric8.kubernetes.client.KubernetesClient;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DefaultAgogosClient implements AgogosClient {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    V1alpha1APIGroupClient v1alpha1Client;

    @Override
    public V1alpha1APIGroup v1alpha1() {
        return v1alpha1Client;
    }

    @Override
    public String namespace() {
        return kubernetesClient.getNamespace();
    }
}
