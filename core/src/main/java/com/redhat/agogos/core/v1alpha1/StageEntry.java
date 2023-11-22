package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class StageEntry implements KubernetesResource {
    private static final long serialVersionUID = -7474791004555510761L;

    static Logger LOG = LoggerFactory.getLogger(StageEntry.class);

    @Getter
    @Setter
    private StageReference stageRef = new StageReference();

    @Getter
    @Setter
    private List<String> runAfter;

    @Getter
    @Setter
    private Map<Object, Object> config = new HashMap<>();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StageEntry)) {
            return false;
        }

        StageEntry stageEntry = (StageEntry) obj;

        return Objects.equals(stageEntry.getStageRef(), getStageRef())
                && Objects.equals(stageEntry.getRunAfter(), getRunAfter())
                && Objects.equals(stageEntry.getConfig(), getConfig());
    }

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageReference implements KubernetesResource {
        private static final long serialVersionUID = -7474791004592310761L;

        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private String namespace;

        @JsonIgnore // TODO: Double check if this works, added because of: Caused by: java.lang.IllegalArgumentException: Found a cyclic reference involving the field stages of type com.redhat.agogos.core.v1alpha1.Pipeline$PipelineSpec.StageEntry
        @JsonBackReference
        @Getter
        @Setter
        public Pipeline pipeline;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StageReference)) {
                return false;
            }

            StageReference stageRef = (StageReference) obj;

            return Objects.equals(stageRef.getName(), getName())
                    && Objects.equals(stageRef.getNamespace(), getNamespace())
                    && Objects.equals(stageRef.getPipeline(), getPipeline());
        }
    }
}
