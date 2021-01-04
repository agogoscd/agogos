package com.redhat.cpaas.k8s.model;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ComponentResourceDoneable extends CustomResourceDoneable<ComponentResource> {

    public ComponentResourceDoneable(final ComponentResource resource,
            final Function<ComponentResource, ComponentResource> function) {
        super(resource, function);
    }

}
