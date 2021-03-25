package com.redhat.cpaas.eventing;

import com.redhat.cpaas.BrokerRestClient;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CloudEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(CloudEventPublisher.class);

    /**
     * Broker RestClient.
     */
    @Inject
    @RestClient
    BrokerRestClient broker;

    @ConfigProperty(name = "agogos.cloud-events.publish")
    Optional<Boolean> publish;

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

    public void publish(CloudEventType type, String data) {
        publish(type.getValue(), data);
    }
}
