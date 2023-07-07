package com.redhat.agogos.core;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class DefaultObjectMapperCustomizer implements KubernetesClientObjectMapperCustomizer, ObjectMapperCustomizer {
    public void customize(ObjectMapper mapper) {
        mapper.setConfig(mapper.getDeserializationConfig().with(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    }
}
