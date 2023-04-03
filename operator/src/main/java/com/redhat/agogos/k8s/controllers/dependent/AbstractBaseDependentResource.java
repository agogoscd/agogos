package com.redhat.agogos.k8s.controllers.dependent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.AgogosResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.client.TektonClient;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public abstract class AbstractBaseDependentResource<R extends HasMetadata, P extends HasMetadata>
        extends CRUDKubernetesDependentResource<R, P> {

    public AbstractBaseDependentResource(Class<R> resourceType) {
        super(resourceType);
    }

    @Inject
    TektonClient tektonClient;

    @Inject
    AgogosClient agogosClient;

    @Inject
    ObjectMapper objectMapper;

    protected AgogosResource<?, ?> parentResource(P resource) {
        throw new ApplicationException("No implementation of parentResource for '{}'", resource.getKind());
    }
}