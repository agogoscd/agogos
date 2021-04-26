package com.redhat.agogos.k8s.client;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DefaultAgogosClient implements AgogosClient {

    @Inject
    V1alpha1APIGroupClient v1alpha1Client;

    @Override
    public V1alpha1APIGroup v1alpha1() {
        return v1alpha1Client;
    }
}
