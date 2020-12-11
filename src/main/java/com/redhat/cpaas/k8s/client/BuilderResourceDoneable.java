package com.redhat.cpaas.k8s.client;

import com.redhat.cpaas.k8s.model.BuilderResource;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class BuilderResourceDoneable extends CustomResourceDoneable<BuilderResource> {

    public BuilderResourceDoneable(final BuilderResource resource, final Function<BuilderResource, BuilderResource> function) {
        super(resource, function);
    }
}
