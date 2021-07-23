package com.redhat.agogos;

import io.cloudevents.CloudEvent;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@RegisterRestClient(configKey = "broker")
public interface BrokerRestClient {

    @POST
    @Path("/agogos")
    @Produces(MediaType.APPLICATION_JSON)
    void sendEvent(final CloudEvent cloudEvent);
}
