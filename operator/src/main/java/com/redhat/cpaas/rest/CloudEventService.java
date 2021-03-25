package com.redhat.cpaas.rest;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "cloud-events-service")
@Singleton
public interface CloudEventService {
    String publish();
}
