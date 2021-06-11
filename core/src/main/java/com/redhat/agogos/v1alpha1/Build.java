package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.Build.BuildSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
@Kind("Build")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Build extends AgogosResource<BuildSpec, ResultableStatus> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuildSpec implements KubernetesResource {
        private static final long serialVersionUID = 4609282852879403086L;

        @Getter
        @Setter
        private String component;
    }

    public Build() {
        super();
    }

    @Getter
    @Setter
    private BuildSpec spec = new BuildSpec();

    @Override
    protected ResultableStatus initStatus() {
        return new ResultableStatus();
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
