package com.redhat.agogos;

import io.cloudevents.CloudEvent;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/agogos")
@RegisterRestClient(configKey = "broker")
public interface BrokerRestClient {

    @POST
    @Path("/default")
    @Produces(MediaType.APPLICATION_JSON)
    void sendEvent(final CloudEvent cloudEvent);
}
