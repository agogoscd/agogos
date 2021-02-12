package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class ComponentGroupResourceList extends CustomResourceList<ComponentGroupResource> {

    private static final long serialVersionUID = 3041862949098261683L;

}
