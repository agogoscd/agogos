package com.redhat.agogos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.cloudevents.jackson.JsonFormat;
import io.quarkiverse.operatorsdk.runtime.KubernetesClientSerializationCustomizer;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
@KubernetesClientSerializationCustomizer
public class DefaultObjectMapperCustomizer implements ObjectMapperCustomizer {
    public void customize(ObjectMapper mapper) {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    }
}
