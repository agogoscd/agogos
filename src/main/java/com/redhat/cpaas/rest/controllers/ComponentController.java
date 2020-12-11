package com.redhat.cpaas.rest.controllers;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.cpaas.ApplicationException;
import com.redhat.cpaas.MissingResourceException;
import com.redhat.cpaas.k8s.client.BuildResourceClient;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.client.TektonResourceClient;
import com.redhat.cpaas.model.Build;
import com.redhat.cpaas.model.Component;

@Path("/components")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComponentController {

    @Inject
    ComponentResourceClient componentResourceClient;

    @Inject
    BuildResourceClient buildResourceClient;

    @Inject
    TektonResourceClient tektonResourceClient;

    @GET
    public List<Component> list() {
        return componentResourceClient.list().stream().map(item -> new Component(item)).collect(Collectors.toList());
    }

    /**
     * Creates a new Component within the system
     * 
     * A new Tekton Pipeline is created and associated with this controller as well.
     * 
     * @param component The component to create.
     * @return
     * @throws ApplicationException
     */
    @POST
    public Response create(Component component) throws ApplicationException {
        component = new Component(componentResourceClient.create(component));

        return Response.ok(component).status(200).build();
    }

    @GET
    @Path("/{name}")
    public Component getComponent(@NotNull @PathParam("name") String name) throws MissingResourceException {
        return new Component(componentResourceClient.getByName(name));
    }

    /**
     * Triggers a build of specific component.
     * 
     * @param name Name of the component to build.
     * @return
     * @throws MissingResourceException
     */
    @POST
    @Path("/{name}/build")
    public Build buildComponent(@PathParam("name") String componentName) throws ApplicationException {
        return new Build(buildResourceClient.create(componentName));
    }

    /**
     * Returns list of builds for a particular component.
     * 
     * @param name Name of the component
     * @return
     * @throws MissingResourceException
     */
    @GET
    @Path("/{name}/builds")
    public List<Build> listBuilds(@PathParam("name") String componentName) throws MissingResourceException {
        return buildResourceClient.listBuilds(componentName).stream().map(item -> new Build(item))
                .collect(Collectors.toList());

    }
}
