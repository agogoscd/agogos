package com.redhat.agogos.operator.k8s.controllers;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.k8s.client.AgogosClient;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.tekton.client.TektonClient;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public abstract class AbstractDependentResource<R extends HasMetadata, P extends HasMetadata>
        extends CRUDKubernetesDependentResource<R, P> {

    public AbstractDependentResource(Class<R> resourceType) {
        super(resourceType);
    }

    @Inject
    protected TektonClient tektonClient;

    @Inject
    protected AgogosClient agogosClient;

    @Inject
    protected KubernetesSerialization objectMapper;

    protected AgogosResource<?, ?> parentResource(P resource) {
        throw new ApplicationException("No implementation of parentResource for '{}'", resource.getKind());
    }
}