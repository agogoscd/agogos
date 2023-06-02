package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize
public class BuildList extends DefaultKubernetesResourceList<Build> {
    private static final long serialVersionUID = 9154628827053441220L;
}
