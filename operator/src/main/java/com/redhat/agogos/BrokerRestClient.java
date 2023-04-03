package com.redhat.agogos;

import io.cloudevents.CloudEvent;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "broker")
public interface BrokerRestClient {

    @POST
    @Path("/agogos")
    @Produces(MediaType.APPLICATION_JSON)
    void sendEvent(final CloudEvent cloudEvent);
}
