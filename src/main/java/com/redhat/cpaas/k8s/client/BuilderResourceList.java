package com.redhat.cpaas.k8s.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.k8s.model.BuilderResource;

import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class BuilderResourceList extends CustomResourceList<BuilderResource> {

    private static final long serialVersionUID = 3743728980437491482L;
}
