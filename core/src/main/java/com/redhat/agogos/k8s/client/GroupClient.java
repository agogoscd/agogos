package com.redhat.agogos.k8s.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.v1alpha1.Group;
import com.redhat.agogos.v1alpha1.GroupList;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RegisterForReflection
public class GroupClient {
    private static final Logger LOG = LoggerFactory.getLogger(GroupClient.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    NonNamespaceOperation<Group, GroupList, Resource<Group>> componentGroupClient;

    @PostConstruct
    void init() {
        componentGroupClient = kubernetesClient.customResources(Group.class,
                GroupList.class);
    }

    /**
     * Find the {@link Group} by name.
     * 
     * @param name Name of the ComponentGroup.
     * @return The {@link Group} or <code>null</code> in case it
     *         cannot be found
     */
    public Group getByName(String name) {
        ListOptions options = new ListOptionsBuilder().withFieldSelector(String.format("metadata.name=%s", name))
                .build();

        GroupList componentGroupResources = componentGroupClient.list(options);

        if (componentGroupResources.getItems().isEmpty() || componentGroupResources.getItems().size() > 1) {
            LOG.debug("ComponentGroup '{}' cannot be found", name);
            return null;
        }

        return componentGroupResources.getItems().get(0);
    }
}
