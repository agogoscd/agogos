package com.redhat.agogos.core.k8s.client;

import com.redhat.agogos.core.KubernetesFacade;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@RegisterForReflection
public class DefaultAgogosClient implements AgogosClient {

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    V1alpha1APIGroupClient v1alpha1Client;

    @Override
    public V1alpha1APIGroup v1alpha1() {
        return v1alpha1Client;
    }

    @Override
    public String namespace() {
        return kubernetesFacade.getNamespace();
    }
}
