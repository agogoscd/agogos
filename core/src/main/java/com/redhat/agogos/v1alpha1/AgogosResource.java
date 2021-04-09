package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.client.CustomResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgogosResource<S, T> extends CustomResource<S, T> {

    protected static final Logger LOG = LoggerFactory.getLogger(AgogosResource.class);

    private static final long serialVersionUID = -1042563779571490559L;

    /**
     * <p>
     * Returns object name together with namespace if available. Useful for logging.
     * </p>
     * 
     * @return String in format: <code>[NAMESPACE]/[NAME]</code> or
     *         <code>[NAME]</code> in case resource is cluster scoped
     */
    @JsonIgnore
    public String getFullName() {
        if (this.getMetadata().getNamespace() != null) {
            return this.getMetadata().getNamespace() + "/" + this.getMetadata().getName();
        }

        return this.getMetadata().getName();
    }

    @JsonIgnore
    // TODO: add logic based on status
    public boolean isReady() {
        return true;
    }

    // @JsonIgnore
    // public CloudEventType event(PipelineRunStatus status, AgogosResource<?, ?> parent) {
    //     if (status == null) {
    //         return null;
    //     }

    //     ObjectMapper objectMapper = new ObjectMapper();

    //     Map<String, ? extends AgogosResource<?, ?>> payload = Map.of(this.getKind().toLowerCase(), this,
    //             parent.getKind().toLowerCase(), parent);

    //     JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

    //     payload.forEach((key, o) -> {
    //         try {
    //             dataBuilder.add(key,
    //                     Json.createReader(new StringReader(objectMapper.writeValueAsString(o))).readValue());
    //         } catch (JsonProcessingException e) {
    //             throw new ApplicationException("Error while preparing CloudEvent data for '{}' key and '{}' object",
    //                     key, o, e);
    //         }
    //     });

    //     String type = String.format("com.redhat.agogos.event.%s.%s.v1alpha1", this.getKind().toLowerCase(),
    //             status.toEvent().toString().toLowerCase());
    //     String data = dataBuilder.build().toString();
    // }
}
