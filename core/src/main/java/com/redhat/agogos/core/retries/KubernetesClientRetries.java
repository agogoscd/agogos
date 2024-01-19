package com.redhat.agogos.core.retries;

import com.redhat.agogos.core.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryConfig.Builder;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class KubernetesClientRetries {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClientRetries.class);

    private enum Command {
        CREATE("create"),
        FORCE_SERVER_SIDE_APPLY("forceServerSideApply"),
        SERVER_SIDE_APPLY("serverSideApply"),
        UPDATE("update");

        private final String printable;

        private Command(String printable) {
            this.printable = printable;
        }

        @Override
        public String toString() {
            return printable;
        }
    }

    private static final String ALL_PODS_RUNNING_PHASE = "Running";
    private static final Integer ALL_PODS_RUNNING_MAX_INTERVAL = 5;
    private static final Integer ALL_PODS_RUNNING_MAX_RETRIES = 36;
    private static final Integer DEFAULT_MAX_INTERVAL = 2;
    private static final Integer DEFAULT_MAX_RETRIES = 10;

    @Inject
    KubernetesClient kubernetesClient;

    public <T extends HasMetadata> T create(T resource) {
        return createOrUpdate(resource, Command.CREATE, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public <T extends HasMetadata> T create(T resource, Integer retries, Integer interval) {
        return createOrUpdate(resource, Command.CREATE, retries, interval);
    }

    public <T extends HasMetadata> T serverSideApply(T resource) {
        return serverSideApply(resource, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public <T extends HasMetadata> T serverSideApply(T resource, Integer retries, Integer interval) {
        return serverSideApply(resource, Command.SERVER_SIDE_APPLY, retries, interval);
    }

    public <T extends HasMetadata> T forceServerSideApply(T resource) {
        return forceServerSideApply(resource, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public <T extends HasMetadata> T forceServerSideApply(T resource, Integer retries, Integer interval) {
        return serverSideApply(resource, Command.FORCE_SERVER_SIDE_APPLY, retries, interval);
    }

    private <T extends HasMetadata> T serverSideApply(T resource, Command command, Integer retries, Integer interval) {
        return createOrUpdate(resource, command, retries, interval);
    }

    public <T extends HasMetadata> T update(T resource) {
        return createOrUpdate(resource, Command.UPDATE, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public <T extends HasMetadata> T update(T resource, Integer retries, Integer interval) {
        return createOrUpdate(resource, Command.UPDATE, retries, interval);
    }

    private <T extends HasMetadata> T createOrUpdate(T resource, Command command, Integer retries, Integer interval) {
        RetryConfig config = RetryConfig.<T> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryOnResult(r -> r == null)
                .retryExceptions(KubernetesClientException.class)
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName(command.toString(), retries, interval));
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying {} for {}", command, resource.getMetadata().getName()));

        Function<T, T> decorated = Retry.decorateFunction(retry, (T r) -> {
            switch (command) {
                case CREATE:
                    return kubernetesClient.resource(r).create();
                case FORCE_SERVER_SIDE_APPLY:
                    return kubernetesClient.resource(r).forceConflicts().serverSideApply();
                case SERVER_SIDE_APPLY:
                    return kubernetesClient.resource(r).serverSideApply();
                case UPDATE:
                    return kubernetesClient.resource(r).update();
                default:
                    throw new ApplicationException("Unhandled command: " + command);
            }
        });

        return decorated.apply(resource);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(T resource) {
        return delete(resource, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(T resource, Integer retries, Integer interval) {
        RetryConfig config = RetryConfig.<List<StatusDetails>> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName("delete-resource", retries, interval));
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying delete for {}",
                        resource.getMetadata().getNamespace() + "/" + resource.getMetadata().getName()));

        Supplier<List<StatusDetails>> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.resource(resource).delete();
        });

        return decorated.get();
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String namespace, String name) {
        return delete(clazz, namespace, name, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String namespace, String name,
            Integer retries, Integer interval) {
        RetryConfig config = RetryConfig.<List<StatusDetails>> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName("delete", retries, interval));
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying delete for {}",
                        (namespace != null ? namespace + "/" : "") + name));

        Supplier<List<StatusDetails>> decorated = Retry.decorateSupplier(retry, () -> {
            if (namespace == null) {
                return kubernetesClient.resources(clazz).withName(name).delete();
            }

            return kubernetesClient.resources(clazz).inNamespace(namespace).withName(name).delete();
        });

        return decorated.get();
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name, Boolean retryOnNull) {
        return get(clazz, namespace, name, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL, retryOnNull);
    }

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name, Integer retries,
            Integer interval, Boolean retryOnNull) {
        Builder<T> builder = RetryConfig.<T> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryExceptions(KubernetesClientException.class);

        if (retryOnNull) {
            builder.retryOnResult(response -> response == null);
        }
        RetryConfig config = builder.build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName("get", retries, interval, retryOnNull));
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying get for {}",
                        (namespace != null ? namespace + "/" : "") + name));

        Supplier<T> decorated = Retry.decorateSupplier(retry, () -> {
            if (namespace == null) {
                return kubernetesClient.resources(clazz).withName(name).get();
            }
            return kubernetesClient.resources(clazz).inNamespace(namespace).withName(name).get();
        });

        return decorated.get();
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options,
            boolean retryOnEmptyList) {
        return list(clazz, namespace, options, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL, retryOnEmptyList);
    }

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options,
            Integer retries, Integer interval, boolean retryOnEmptyList) {
        Builder<List<T>> builder = RetryConfig.<List<T>> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryExceptions(KubernetesClientException.class);

        if (retryOnEmptyList) {
            builder.retryOnResult(response -> response.size() == 0);
        }
        RetryConfig config = builder.build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName("list-resource", retries, interval, retryOnEmptyList));
        retry.getEventPublisher()
                .onRetry(e -> {
                    if (retryOnEmptyList) {
                        LOG.warn("⚠️ WARN: Retrying list for {}", namespace);
                    }
                });

        Supplier<List<T>> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.resources(clazz).inNamespace(namespace).list(options).getItems();
        });

        return decorated.get();
    }

    public List<HasMetadata> list(String namespace, ListOptions options) {
        return list(namespace, options, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public List<HasMetadata> list(String namespace, ListOptions options, Integer retries, Integer interval) {
        RetryConfig config = RetryConfig.<List<HasMetadata>> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName("list-namespace", retries, interval));
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying list for {}", namespace));

        Supplier<List<HasMetadata>> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.resources(HasMetadata.class).inNamespace(namespace).list(options).getItems();
        });

        return decorated.get();
    }

    public <T extends HasMetadata> List<GenericKubernetesResource> getKubernetesResources(String namespace,
            String groupVersion, String kind, ListOptions options) {
        return getKubernetesResources(namespace, groupVersion, kind, options, DEFAULT_MAX_RETRIES, DEFAULT_MAX_INTERVAL);
    }

    public List<GenericKubernetesResource> getKubernetesResources(String namespace,
            String groupVersion, String kind, ListOptions options, Integer retries, Integer interval) {
        Builder<Set<String>> builder = RetryConfig.<Set<String>> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryExceptions(KubernetesClientException.class);

        RetryConfig config = builder.build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("get-kubernetes-resources");
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying getKubernetesResources for {} {}", groupVersion, kind));

        Supplier<List<GenericKubernetesResource>> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.genericKubernetesResources(groupVersion, kind)
                    .inNamespace(namespace)
                    .list(options)
                    .getItems();
        });

        return decorated.get();
    }

    public Deployment restartDeployment(String namespace, String name) {
        RetryConfig config = RetryConfig.<EventListener> custom()
                .maxAttempts(DEFAULT_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(DEFAULT_MAX_INTERVAL))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("restart-deployment");
        retry.getEventPublisher()
                .onRetry(e -> LOG.info("⏳ WAIT: Waiting for Deployment '{}' in namespace '{}' to be restarted",
                        name, namespace));
        Supplier<Deployment> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).rolling().restart();
        });

        LOG.info("👉 OK: Deployment '{}' in namespace '{}' has been restarted", name, namespace);
        return (Deployment) decorated.get();
    }

    public void waitForAllPodsRunning(String namespace) {
        waitForAllPodsRunning(namespace, ALL_PODS_RUNNING_MAX_RETRIES, ALL_PODS_RUNNING_MAX_INTERVAL);
    }

    public void waitForAllPodsRunning(String namespace, Integer retries, Integer interval) {
        RetryConfig config = RetryConfig.<Set<String>> custom()
                .maxAttempts(retries)
                .waitDuration(Duration.ofSeconds(interval))
                .retryOnResult(phases -> phases.size() != 1 || !phases.contains(ALL_PODS_RUNNING_PHASE))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(getRegistryRetryName("pod-phasees", retries, interval));
        retry.getEventPublisher()
                .onRetry(e -> LOG.info("⏳ WAIT: Waiting for all pods in the '{}' namespace to be {}", namespace,
                        ALL_PODS_RUNNING_PHASE));
        Supplier<Set<String>> decorated = Retry.decorateSupplier(retry, () -> {
            return getAllPodPhases(namespace);
        });

        Set<String> result = (Set<String>) decorated.get();
        if (allPodsRunning(result)) {
            LOG.info("👉 OK: All pods in the '{}' namespace are {}", namespace, ALL_PODS_RUNNING_PHASE);
        } else {
            LOG.warn("⚠️ WARN: All pods in the '{}' namespace are not yet {}", namespace, ALL_PODS_RUNNING_PHASE);
        }
    }

    public EventListener waitForEventListenerRunning(EventListener el) {
        String name = el.getMetadata().getName();
        String namespace = el.getMetadata().getNamespace();

        RetryConfig config = RetryConfig.<EventListener> custom()
                .maxAttempts(ALL_PODS_RUNNING_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(ALL_PODS_RUNNING_MAX_INTERVAL))
                .retryOnResult(listener -> listener.getStatus().getAddress().getUrl() == null)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("el-running");
        retry.getEventPublisher()
                .onRetry(e -> LOG.info("⏳ WAIT: Waiting for EventListener '{}' in namespace '{}' to be running",
                        name, namespace));
        Supplier<EventListener> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.resources(EventListener.class).inNamespace(namespace).withName(name).get();
        });

        EventListener result = (EventListener) decorated.get();
        if (result.getStatus().getAddress().getUrl() != null) {
            LOG.info("👉 OK: EventListener '{}' in namespace '{}' is running", name, namespace);
        } else {
            LOG.warn("⚠️ WARN: EventListener '{}' in namespace '{}' is not running", name, namespace);
        }
        return result;
    }

    private Set<String> getAllPodPhases(String namespace) {
        return kubernetesClient.pods()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(p -> p.getStatus().getPhase())
                .collect(Collectors.toSet());
    }

    private boolean allPodsRunning(Set<String> phases) {
        return phases.size() == 1 && phases.contains(ALL_PODS_RUNNING_PHASE);
    }

    private String getRegistryRetryName(String operation, Integer retries, Integer interval) {
        return getRegistryRetryName(operation, retries, interval, null);
    }

    private String getRegistryRetryName(String operation, Integer retries, Integer interval, Boolean check) {
        return String.format("%s-%d-%d%s", operation, retries, interval, (check != null ? "-" + check : ""));
    }
}
