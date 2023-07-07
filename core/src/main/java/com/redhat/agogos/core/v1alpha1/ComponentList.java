package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize
@RegisterForReflection
public class ComponentList extends DefaultKubernetesResourceList<Component> {
    private static final long serialVersionUID = 9154628827053441220L;
}
