package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize
public class BuildResourceList extends CustomResourceList<Build> {
    private static final long serialVersionUID = 9154628827053441220L;
}
