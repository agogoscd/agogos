package com.redhat.agogos.operator.k8s.controllers;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.k8s.client.AgogosClient;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.operator.eventing.CloudEventPublisher;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.tekton.client.TektonClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractController<T extends AgogosResource<?, ?>>
        implements Namespaced, Reconciler<T>, Cleaner<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);

    @Inject
    protected AgogosClient agogosClient;

    @Inject
    protected CloudEventPublisher cloudEventPublisher;

    @Inject
    protected KubernetesSerialization objectMapper;

    @Inject
    protected TektonClient tektonClient;

    @Override
    public UpdateControl<T> reconcile(T resource, Context<T> context) {
        LOG.info("Checking {} '{}', returning noUpdate", resource.getKind(), resource.getFullName());
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(T resource, Context<T> context) {
        LOG.info("Removing {} '{}'", resource.getKind(), resource.getFullName());
        return DeleteControl.defaultDelete();
    }

    protected AgogosResource<?, ?> parentResource(T resource, Context<T> context) {
        throw new ApplicationException("No implementation of parentResource for '{}'", resource.getKind());
    }
}
