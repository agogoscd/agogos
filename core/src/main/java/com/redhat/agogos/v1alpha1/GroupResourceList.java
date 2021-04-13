package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class GroupResourceList extends CustomResourceList<GroupResource> {

    private static final long serialVersionUID = 3041862949098261683L;

}
