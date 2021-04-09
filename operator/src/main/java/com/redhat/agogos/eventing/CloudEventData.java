package com.redhat.agogos.eventing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.PipelineRunStatus;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.v1alpha1.CloudEventType;
import com.redhat.agogos.v1alpha1.StatusResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import java.io.StringReader;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import lombok.Getter;

public class CloudEventData {
    @Getter
    CloudEventType type;

    @Getter
    String data;

    CloudEventData(CloudEventType type, String data) {
        this.type = type;
        this.data = data;
    }

    CloudEventData(CustomResource<?, StatusResource> resource, PipelineRunStatus status) {
        String type = resource.getMetadata().getLabels().get(Resource.RESOURCE.getLabel());

        //CloudEventType.forResource(Resource.fromType(type), status);
    }

    CloudEventData(CloudEventType type, Map<String, ? extends HasMetadata> data) {
        this.type = type;

        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

        data.forEach((key, o) -> {
            try {
                dataBuilder.add(key,
                        Json.createReader(new StringReader(new ObjectMapper().writeValueAsString(o))).readValue());
            } catch (JsonProcessingException e) {
                throw new ApplicationException("Error while preparing CloudEvent data for '{}' key and '{}' object",
                        key, o, e);
            }
        });

        this.data = dataBuilder.build().toString();
    }
}
