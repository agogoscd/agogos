package com.redhat.agogos.core.eventing;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.PipelineRunState;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class BaseCloudEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(BaseCloudEventPublisher.class);

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    KubernetesSerialization objectMapper;

    @ConfigProperty(name = "agogos.cloud-events.base-url", defaultValue = "http://broker-ingress.knative-eventing.svc.cluster.local")
    String baseurl;

    @ConfigProperty(name = "agogos.cloud-events.publish", defaultValue = "true")
    Boolean publish;

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

    public void publish(String namespace, String state, Class<? extends HasMetadata> clazz, SubmissionSpec spec) {
        if (state == null || spec == null) {
            LOG.warn("Missing one of required resources, CloudEvent won't be published!");
            return;
        }
        AgogosResource<?, ?> resources[] = {};

        publish(namespace, CloudEventHelper.type(clazz, state), spec, resources);
    }

    public void publish(PipelineRunState state, AgogosResource<?, ?>... resources) {
        if (state == null || resources.length == 0) {
            LOG.warn("Missing one of required resources, CloudEvent won't be published!",
                    new RuntimeException("Missing required resources"));
            return;
        }

        String type = CloudEventHelper.type(resources[0].getClass(), state);
        publish(resources[0].getMetadata().getNamespace(), type, null, resources);
    }

    public void publish(Execution execution) {
        if (execution == null) {
            LOG.warn("Missing the required Execution resource, CloudEvent won't be published!",
                    new RuntimeException("Missing Execution resource"));
            return;
        }

        String type = CloudEventHelper.type(execution);
        publish(execution.getMetadata().getNamespace(), type, null, execution);
    }

    private void publish(String namespace, String type, SubmissionSpec spec, AgogosResource<?, ?>... resources) {

        Map<String, ? extends AgogosResource<?, ?>> payload = Arrays.asList(resources).stream()
                .collect(Collectors.toMap(resource -> resource.getKind().toLowerCase(), resource -> resource));

        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

        payload.forEach((key, o) -> {
            dataBuilder.add(key, Json.createReader(new StringReader(objectMapper.asJson(o))).readValue());
        });

        if (spec != null) {
            @SuppressWarnings(value = { "unchecked" })
            Map<String, String> values = objectMapper.convertValue(spec, Map.class);
            values.entrySet().forEach((e -> {
                dataBuilder.add(e.getKey(), e.getValue());
            }));
        }

        String data = dataBuilder.build().toString();

        final var eventData = PojoCloudEventData.wrap(payload, (p) -> data.getBytes());
        BrokerRestClient client = restClient(namespace);

        send(client, type, eventData);
    }

    private void send(BrokerRestClient broker, String type, CloudEventData data) {
        if (!publish) {
            LOG.debug(
                    "Publishing CloudEvent '{}' skipped because it is disabled in configuration; see 'agogos.cloud-events.publish' property",
                    type);
            return;
        }

        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withType(type)
                .withSource(URI.create("http://localhost")) // This seems to work, at least for now.
                .withId(UUID.randomUUID().toString())
                .withDataContentType(MediaType.APPLICATION_JSON)
                .withData(data);

        CloudEvent cloudEvent = cloudEventBuilder.build();

        LOG.debug("Sending '{}' CloudEvent", type);
        LOG.debug("CloudEvent: '{}'", objectMapper.asJson(cloudEvent));

        Response response = broker.sendEvent(cloudEvent);
        LOG.debug("CloudEvent Response: {}", objectMapper.asJson(response.getHeaders()));
    }
}
