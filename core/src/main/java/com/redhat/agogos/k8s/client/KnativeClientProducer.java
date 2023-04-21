package com.redhat.agogos.k8s.client;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.Config;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class KnativeClientProducer {

    @DefaultBean
    @Singleton
    @Produces
    public KnativeClient knativeClient(Config config) {
        return new DefaultKnativeClient(config);
    }

}
