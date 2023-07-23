package com.redhat.agogos.core.eventing;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class CloudEventHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> updatedHeaders = new MultivaluedHashMap<>();
        updatedHeaders.putAll(clientOutgoingHeaders);
        updatedHeaders.putSingle("Content-Type", "application/cloudevents+json");
        return updatedHeaders;
    }
}
