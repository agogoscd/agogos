package com.redhat.agogos.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Path("/")
@RegisterRestClient(configKey = "broker")
@Singleton
public interface CloudEventService {
    String publish();
}
