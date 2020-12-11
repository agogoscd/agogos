package com.redhat.cpaas.rest.controllers;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.model.Pipeline;

@Path("/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PipelineController {
    @Inject
    TektonResourceClient tektonResourceClient;

    @GET
    public List<Pipeline> list() {
        return tektonResourceClient.listPipelines().stream().map(item -> new Pipeline(item))
                .collect(Collectors.toList());
    }

    // @POST
    // @Path("run/{name}")
    // public PipelineRun run(@PathParam("name") String name) throws ApplicationException {
    //     return new PipelineRun(tektonResourceClient.runPipeline(name, ""));
    // }

}
