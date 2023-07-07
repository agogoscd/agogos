package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Group.GroupResourceSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Group")
@io.fabric8.kubernetes.model.annotation.Group("agogos.redhat.com")
@Version("v1alpha1")
public class Group extends AgogosResource<GroupResourceSpec, Void> implements Namespaced {
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class GroupResourceSpec {

        @Getter
        @Setter
        private List<String> components = new ArrayList<>();

        @Getter
        @Setter
        private List<String> pipelines = new ArrayList<>();
    }

    private static final long serialVersionUID = -7092342726608099745L;

    @Override
    protected GroupResourceSpec initSpec() {
        return new GroupResourceSpec();
    }
}
