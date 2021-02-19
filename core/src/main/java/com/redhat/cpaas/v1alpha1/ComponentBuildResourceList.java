package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class ComponentBuildResourceList extends CustomResourceList<ComponentBuildResource> {
    private static final long serialVersionUID = 9154628827053441220L;
}
