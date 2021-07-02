package com.redhat.agogos.relays.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class GitHubRelay {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubRelay.class);

    @POST
    @Path("receive")
    public String receive(String payload) {
        LOG.debug("Webhook received with payload: {}", payload);
        // TODO: Implement this
        return "OK";
    }
}
