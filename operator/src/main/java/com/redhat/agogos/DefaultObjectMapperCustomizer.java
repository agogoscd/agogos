package com.redhat.agogos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class DefaultObjectMapperCustomizer implements KubernetesClientObjectMapperCustomizer {
    public void customize(ObjectMapper mapper) {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    }
}
