package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class ComponentResourceList extends CustomResourceList<ComponentResource> {
    private static final long serialVersionUID = 9154628827053441220L;
}
