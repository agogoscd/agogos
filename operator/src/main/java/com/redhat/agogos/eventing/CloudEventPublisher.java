package com.redhat.agogos.eventing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.BrokerRestClient;
import com.redhat.agogos.CloudEventHelper;
import com.redhat.agogos.PipelineRunState;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.v1alpha1.AgogosResource;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CloudEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(CloudEventPublisher.class);

    // /**
    //  * Broker RestClient.
    //  */
    // @Inject
    // @RestClient
    // BrokerRestClient broker;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "agogos.cloud-events.base-url", defaultValue = "http://broker-ingress.knative-eventing.svc.cluster.local")
    String baseurl = "http://broker-ingress.knative-eventing.svc.cluster.local";

    @ConfigProperty(name = "agogos.cloud-events.publish")
    Optional<Boolean> publish = Optional.of(true);

    Map<String, BrokerRestClient> brokers = new HashMap<>();

    private BrokerRestClient restClient(String namespace) {
        BrokerRestClient client = brokers.get(namespace);

        if (client == null) {
            URL url;

            try {
                url = new URL(
                        new StringBuilder(baseurl)
                                .append("/")
                                .append(namespace)
                                .toString());
            } catch (MalformedURLException e) {
                throw new ApplicationException("Could not create URL for Knative Broker in namespace {}", namespace);
            }

            LOG.debug("Preparing new CloudEvent Broker client with URL '{}'", url);

            client = RestClientBuilder.newBuilder().baseUrl(url).build(BrokerRestClient.class);
            brokers.put(namespace, client);
        }

        return client;
    }

    public void publish(PipelineRunState state, AgogosResource<?, ?> resource, AgogosResource<?, ?> parent) {
        if (state == null || resource == null || parent == null) {
            LOG.warn("Missing one of required resources, CloudEvent won't be published!");
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

        final var eventData = PojoCloudEventData.wrap(payload, (p) -> data.getBytes());
        BrokerRestClient client = restClient(resource.getMetadata().getNamespace());

        send(client, type, eventData);
    }

    private void send(BrokerRestClient broker, String type, CloudEventData data) {
        if (!publish.orElse(true)) {
            LOG.debug(
                    "Publishing CloudEvent '{}' skipped because it is disabled in configuration; see 'agogos.cloud-events.publish' property",
                    type);
            return;
        }

        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withType(type)
                .withSource(URI.create("http://localhost")) // TODO: change this
                .withId(UUID.randomUUID().toString()) // TODO: is this sufficient?
                .withDataContentType(MediaType.APPLICATION_JSON)
                .withData(data);

        CloudEvent cloudEvent = cloudEventBuilder.build();

        LOG.info("Sending '{}' CloudEvent", type);
        LOG.debug("CloudEvent payload: '{}'", data);

        broker.sendEvent(cloudEvent);
    }
}
