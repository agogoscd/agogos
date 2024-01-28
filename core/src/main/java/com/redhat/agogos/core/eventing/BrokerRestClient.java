package com.redhat.agogos.core.eventing;

import io.cloudevents.CloudEvent;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterClientHeaders(CloudEventHeadersFactory.class)
@RegisterRestClient(configKey = "broker")
public interface BrokerRestClient {

    @POST
    @Path("/agogos")
    @Produces(MediaType.APPLICATION_JSON)
    Response sendEvent(final CloudEvent cloudEvent);
}
