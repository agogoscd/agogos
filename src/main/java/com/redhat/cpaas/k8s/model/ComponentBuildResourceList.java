package com.redhat.cpaas.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class ComponentBuildResourceList extends CustomResourceList<ComponentBuildResource> {
    private static final long serialVersionUID = 9154628827053441220L;
}
