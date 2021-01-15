package com.redhat.cpaas.k8s.model;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ComponentGroupResourceDoneable extends CustomResourceDoneable<ComponentGroupResource> {

    public ComponentGroupResourceDoneable(final ComponentGroupResource resource,
            final Function<ComponentGroupResource, ComponentGroupResource> function) {
        super(resource, function);
    }

}
