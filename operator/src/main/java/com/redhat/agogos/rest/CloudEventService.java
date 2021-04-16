package com.redhat.agogos.rest;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "broker")
@Singleton
public interface CloudEventService {
    String publish();
}
