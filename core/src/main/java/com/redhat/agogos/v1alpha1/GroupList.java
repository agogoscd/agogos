package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;

@JsonDeserialize
public class GroupList extends DefaultKubernetesResourceList<Group> {

    private static final long serialVersionUID = 3041862949098261683L;

}
