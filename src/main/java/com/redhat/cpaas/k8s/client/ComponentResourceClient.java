package com.redhat.cpaas.k8s.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.MissingResourceException;
import com.redhat.cpaas.k8s.model.ComponentResource;
import com.redhat.cpaas.k8s.model.ComponentResource.ComponentStatus;
import com.redhat.cpaas.k8s.model.ComponentResourceList;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@Singleton
public class ComponentResourceClient {
    private static final Logger LOG = Logger.getLogger(ComponentResourceClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    BuilderResourceClient builderResourceClient;

    @Inject
    ObjectMapper objectMapper;

    MixedOperation<ComponentResource, ComponentResourceList, Resource<ComponentResource>> componentClient;

    @PostConstruct
    void init() {
        componentClient = kubernetesClient.customResources(ComponentResource.class, ComponentResourceList.class);
    }

    public List<ComponentResource> list() {
        return componentClient.list().getItems();
    }

    /**
     * Updates {@link ComponentResource.ComponentStatus} of the particular
     * {@link ComponentResource}.
     * 
     * This is useful when the are hooks executed which influence the ability of
     * usage of the Component.
     * 
     * @param component {@link ComponentResource} object
     * @param status    One of available statuses
     * @param reason    Description of the reason for last status change
     * @return Updated {@link ComponentResource} object
     */
    public ComponentResource updateStatus(final ComponentResource component, String status, String reason) {
        ComponentStatus componentStatus = component.getStatus();
        componentStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        componentStatus.setStatus(status);
        componentStatus.setReason(reason);

        // Update the Status sub-resource, this will not trigger another reconcile
        return componentClient.updateStatus(component);
    }

    // public ComponentResource create(final Component component) throws
    // ApplicationException {
    // return componentClient.createOrReplace(new ComponentResource(component));
    // }

    public ComponentResource getByName(String name) throws MissingResourceException {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        ComponentResourceList componentResources = null;

        try {
            componentResources = componentClient.list(options);
        } catch (Exception ex) {
            System.out.println(ex);
            LOG.warnv("Could not find component with name ''{0}''", name);
            return null;
        }

        if (componentResources.getItems().isEmpty() || componentResources.getItems().size() > 1) {
            throw new MissingResourceException(String.format("Component by name '%s' not found", name));
        }

        return componentResources.getItems().get(0);
    }
}
