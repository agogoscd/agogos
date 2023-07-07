package com.redhat.agogos.operator.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "broker")
@Singleton
public interface CloudEventService {
    String publish();
}
