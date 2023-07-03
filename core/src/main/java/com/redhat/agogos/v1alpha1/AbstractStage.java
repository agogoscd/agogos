package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.AbstractStage.StageSpec;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public abstract class AbstractStage extends AgogosResource<StageSpec, Status> {

    private static final long serialVersionUID = 7447807439691538160L;

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSchema {
        private static final long serialVersionUID = 5507683698215774978L;

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSpec {

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private TaskRef taskRef = new TaskRef();

        @Getter
        @Setter
        private StageSchema schema = new StageSchema();

    }

    @Override
    protected Status initStatus() {
        return new Status();
    }

    @Override
    protected StageSpec initSpec() {
        return new StageSpec();
    }
}
