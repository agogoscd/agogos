package com.redhat.cpaas.k8s.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.k8s.model.BuildResource;

import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class BuildResourceList extends CustomResourceList<BuildResource> {
    private static final long serialVersionUID = 9154628827053441220L;
}
