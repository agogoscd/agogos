package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.client.CustomResource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgogosResource<S, T> extends CustomResource<S, T> {

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

    @JsonIgnore
    public ZonedDateTime creationTime() {
        return LocalDateTime.parse(this.getMetadata().getCreationTimestamp(),
                DateTimeFormatter.ISO_ZONED_DATE_TIME).atZone(ZoneId.of("UTC"));
    }
}
