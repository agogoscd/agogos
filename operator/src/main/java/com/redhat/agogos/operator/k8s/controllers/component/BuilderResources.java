package com.redhat.agogos.operator.k8s.controllers.component;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class BuilderResources {

    private static final Logger LOG = LoggerFactory.getLogger(BuilderResources.class);

    private static final String SECRET_DOCKER_CONFIG_JSON = "kubernetes.io/dockerconfigjson";

    @Inject
    KubernetesFacade kubernetesFacade;

    @ConfigProperty(name = "agogos.service-account")
    Optional<String> serviceAccount;

    @Inject
    KubernetesSerialization mapper;

    private static String NAMESPACED_LABEL = Label.AGOGOS_LABEL_PREFIX + "namespaced";

    public void sync(Component component) {
        LOG.info("Syncing builder '{}' resources to namespace '{}'", component.getSpec().getBuild().getBuilderRef().getName(),
                component.getMetadata().getNamespace());
        sync(Secret.class, component);
        sync(ConfigMap.class, component);
    }

    public void flush(Component component) {
        // Remove all bulder resources if no longer referenced.
        String builder = component.getSpec().getBuild().getBuilderRef().getName();
        String namespace = component.getMetadata().getNamespace();

        List<Component> components = kubernetesFacade.list(Component.class, namespace).stream()
                .filter(c -> c.getSpec().getBuild().getBuilderRef().getName().equals(builder) &&
                        c.getMetadata().getDeletionTimestamp() != null)
                .toList();

        LOG.error("SIZE OF COMPONENTS => " + components.size());
        if (components.size() == 0) {
            LOG.info("Flushing builder '{}' resources from namespace '{}'",
                    component.getSpec().getBuild().getBuilderRef().getName(),
                    component.getMetadata().getNamespace());

            flush(Secret.class, builder, namespace);
            flush(ConfigMap.class, builder, namespace);
        }
    }

    private <T extends HasMetadata> void sync(Class<T> clazz, Component component) {
        String builder = component.getSpec().getBuild().getBuilderRef().getName();
        String namespace = component.getMetadata().getNamespace();

        Map<String, T> nsResources = getMap(clazz, builder, namespace);
        LOG.error("SIZE NS => " + nsResources.size());
        Map<String, T> builderResources = getMap(clazz, builder, "agogos");
        LOG.error("SIZE BUILDER => " + builderResources.size());
        LOG.error("NS => " + kubernetesFacade.getNamespace());
        Set<String> toRemove = new HashSet<String>(nsResources.keySet());
        toRemove.removeAll(builderResources.keySet());

        // Remove any obsolete resources.
        toRemove.stream().forEach(r -> {
            LOG.info("Removing obsolete builder '{}' {} '{}' to namespace '{}'", builder, clazz.getSimpleName(), r, namespace);
            kubernetesFacade.delete(clazz, namespace, r);
        });

        // Add all builder resources.
        builderResources.values().stream().forEach(r -> {
            LOG.info("Syncing builder '{}' {} '{}' to namespace '{}'", builder, clazz.getSimpleName(),
                    r.getMetadata().getName(), namespace);

            ObjectMeta om = new ObjectMetaBuilder()
                    .withName(r.getMetadata().getName())
                    .withNamespace(namespace)
                    .withLabels(r.getMetadata().getLabels())
                    .build();
            r.setMetadata(om);

            // If it's a pull secret, add it to the SA in the namespace.
            if (isDockerConfigJsonSecret(r)) {
                ServiceAccount sa = kubernetesFacade.get(ServiceAccount.class, namespace, serviceAccount.get());
                if (sa != null) {
                    LOG.info("Adding Secret '{}' to service account", r.getMetadata().getName());
                    LocalObjectReference lor = new LocalObjectReference(namespace);
                    if (!sa.getImagePullSecrets().contains(lor)) {
                        sa.getImagePullSecrets().add(lor);
                        kubernetesFacade.serverSideApply(sa);
                    }
                } else {
                    LOG.error("Unable to find servce account for pull secret.");
                }
            }
            kubernetesFacade.serverSideApply(r);
        });
    }

    private <T extends HasMetadata> void flush(Class<T> clazz, String builder, String namespace) {
        getMap(clazz, builder, namespace).entrySet().stream()
                .forEach(e -> {
                    LOG.info("Removing builder '{}' {} '{}' from namespace '{}'", builder, clazz.getSimpleName(),
                            e.getKey(), namespace);

                    // If it's a pull secret, remove it from the SA in the namespace.
                    if (isDockerConfigJsonSecret(e.getValue())) {
                        ServiceAccount sa = kubernetesFacade.get(ServiceAccount.class, namespace, serviceAccount.get());
                        if (sa != null) {
                            LOG.info("Removing Secret '{}' from service account", e.getValue().getMetadata().getName());
                            LocalObjectReference lor = new LocalObjectReference(namespace);
                            if (!sa.getImagePullSecrets().contains(lor)) {
                                sa.getImagePullSecrets().add(lor);
                                kubernetesFacade.serverSideApply(sa);
                            }
                        } else {
                            LOG.error("Unable to find servce account for pull secret.");
                        }
                    }

                    kubernetesFacade.delete(clazz, namespace, e.getKey());
                });
    }

    private <T extends HasMetadata> Map<String, T> getMap(Class<T> clazz, String builder, String namespace) {
        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(Label.create(Resource.BUILDER) + "=" + builder + "," + NAMESPACED_LABEL + "=true")
                .build();

        return kubernetesFacade.list(clazz, namespace, options)
                .stream()
                .collect(Collectors.toMap(s -> s.getMetadata().getName(), s -> s));
    }

    private boolean isDockerConfigJsonSecret(Object o) {
        return o instanceof Secret && ((Secret) o).getType().equals(SECRET_DOCKER_CONFIG_JSON);
    }
}
