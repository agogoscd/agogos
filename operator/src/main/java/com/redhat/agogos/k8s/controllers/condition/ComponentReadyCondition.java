package com.redhat.agogos.k8s.controllers.condition;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentReadyCondition implements Condition<Component, Build> {

    protected static final Logger LOG = LoggerFactory.getLogger(ComponentReadyCondition.class);

    @Inject
    AgogosClient agogosClient;

    @Override
    public boolean isMet(DependentResource<Component, Build> dependentResource, Build primary, Context<Build> context) {
        String name = primary.getSpec().getComponent();
        String namespace = primary.getMetadata().getNamespace();
        Component component = agogosClient.v1alpha1().components().inNamespace(namespace).withName(name).get();

        if (component != null) {
            LOG.debug("Component '{}/{}' has status {}", namespace, name, component.getStatus().getStatus());
        } else {
            LOG.debug("Component '{}/{}' does not exist.", namespace, name);
        }
        return component != null
                && ResourceStatus.valueOf(component.getStatus().getStatus()) == ResourceStatus.Ready;
    }
}
