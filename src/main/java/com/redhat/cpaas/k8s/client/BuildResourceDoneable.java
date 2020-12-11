package com.redhat.cpaas.k8s.client;

import com.redhat.cpaas.k8s.model.BuildResource;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class BuildResourceDoneable extends CustomResourceDoneable<BuildResource> {

    public BuildResourceDoneable(final BuildResource resource, final Function<BuildResource, BuildResource> function) {
        super(resource, function);
    }
}
