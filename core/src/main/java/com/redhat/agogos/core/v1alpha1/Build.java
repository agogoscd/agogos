package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Build.BuildSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Build")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Build extends AgogosResource<BuildSpec, ResultableBuildStatus> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuildSpec {

        @Getter
        @Setter
        private String component;
    }

    @Override
    protected BuildSpec initSpec() {
        return new BuildSpec();
    }

    @Override
    protected ResultableBuildStatus initStatus() {
        return new ResultableBuildStatus();
    }

    /**
     * <p>
     * Get the result of the build.
     * </p>
     * 
     * @return String formatted result
     */
    @JsonIgnore
    public Map<?, ?> getResult() {
        return this.getStatus().getResult();
    }
}
