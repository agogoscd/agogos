package com.redhat.agogos.k8s.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.eventing.CloudEventPublisher;
import com.redhat.agogos.k8s.TektonPipelineHelper;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.ResultableStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.tekton.client.TektonClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public abstract class AbstractController<T extends AgogosResource<?, ?>>
        implements Namespaced, Reconciler<T>, Cleaner<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);

    @Inject
    TektonClient tektonClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CloudEventPublisher cloudEventPublisher;

    @Inject
    TektonPipelineHelper pipelineHelper;

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

    protected ResultableStatus deepCopy(ResultableStatus status) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(status), ResultableStatus.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Could not serialize status object: '{}'", status);
        }
    }
}
