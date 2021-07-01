package com.redhat.agogos.cli;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.errors.ApplicationException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
class ResourceMapping {
    String group;
    String version;
    String kind;
    List<String> endpoints = new ArrayList<>();

    /**
     * <p>
     * Detects the plural of the resource. Plurals are part of the endpoint URL's. This function iterates
     * over them and finds the main one and returns it. This is the only way AFAIK
     * to do this.
     * </p>
     * 
     * @return
     */
    @JsonIgnore
    public String getPlural() {
        String url = endpoints.stream().filter(e -> !e.contains("{")).findAny().orElse(null);

        if (url == null) {
            return null;
        }

        String[] parts = url.split("/");

        return parts[parts.length - 1];
    }

    /**
     * <p>
     * Detects whether the resource is namespaced scoped or not.
     * </p>
     * 
     * @return
     */
    @JsonIgnore
    public boolean isNamespaced() {
        String plural = getPlural();

        if (plural == null) {
            throw new ApplicationException("Could not find plural for resource '{}'", this);
        }

        StringBuilder nsPathBuilder = new StringBuilder();

        if (group == null || group.isBlank()) {
            nsPathBuilder.append("/api");
        } else {
            nsPathBuilder.append("/apis/").append(group);
        }

        nsPathBuilder.append("/").append(version).append("/namespaces/{namespace}/").append(plural);

        String nsPath = nsPathBuilder.toString();

        if (endpoints.contains(nsPath)) {
            return true;
        }

        return false;
    }
}
