package com.redhat.cpaas.rest.controllers;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.cpaas.k8s.client.TektonResourceClient;

@Path("/pipelineruns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PipelineRunController {
    @Inject
    TektonResourceClient tektonResourceClient;

}
