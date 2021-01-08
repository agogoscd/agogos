package com.redhat.cpaas.rest.controllers;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.cpaas.k8s.client.BuilderResourceClient;
import com.redhat.cpaas.k8s.model.BuilderResource;
import com.redhat.cpaas.model.Builder;

@Path("/builders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BuilderController {

    @Inject
    BuilderResourceClient client;

    @GET
    public List<Builder> list() {
        return client.list().stream().map(item -> new Builder(item)).collect(Collectors.toList());
    }

    @POST
    public Builder create(final Builder builder) {
        return new Builder(client.create(new BuilderResource(builder)));
    }
}
