package com.redhat.agogos.eventing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.BrokerRestClient;
import com.redhat.agogos.CloudEventHelper;
import com.redhat.agogos.PipelineRunState;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.v1alpha1.AgogosResource;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;

import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CloudEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(CloudEventPublisher.class);

    /**
     * Broker RestClient.
     */
    @Inject
    @RestClient
    BrokerRestClient broker;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "agogos.cloud-events.publish")
    Optional<Boolean> publish;

    public void publish(PipelineRunState state, AgogosResource<?, ?> resource, AgogosResource<?, ?> parent) {
        if (state == null || resource == null || parent == null) {
            LOG.warn("Missing one of required resources");
            return;
        }

        Map<String, ? extends AgogosResource<?, ?>> payload = Map.of(resource.getKind().toLowerCase(), resource,
                parent.getKind().toLowerCase(), parent);

        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

        payload.forEach((key, o) -> {
            try {
                dataBuilder.add(key,
                        Json.createReader(new StringReader(objectMapper.writeValueAsString(o))).readValue());
            } catch (JsonProcessingException e) {
                throw new ApplicationException("Error while preparing CloudEvent data for '{}' key and '{}' object",
                        key, o, e);
            }
        });

        String type = CloudEventHelper.type(resource.getClass(), state);
        String data = dataBuilder.build().toString();

        publish(type, data);
    }

    public void publish(String type, String data) {
        if (!publish.orElse(true)) {
            LOG.debug(
                    "Publishing CloudEvent '{}' skipped because it is disabled in configuration; see 'agogos.cloud-events.publish' property",
                    type);
            return;
        }

        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1() //
                .withType(type) //
                .withSource(URI.create("http://localhost")) // TODO: change this
                .withId(UUID.randomUUID().toString()) // TODO: is this sufficient?
                .withDataContentType(MediaType.APPLICATION_JSON) //
                .withData(data.getBytes());

        CloudEvent cloudEvent = cloudEventBuilder.build();

        LOG.info("Sending '{}' CloudEvent", type);
        LOG.debug("CloudEvent payload: '{}'", data);

        broker.sendEvent(cloudEvent);

    }
}
