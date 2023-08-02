package com.redhat.agogos.core.retries;

import com.redhat.agogos.core.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
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
    private static final Integer DEFAULT_MAX_INTERVAL = 5;
    private static final Integer DEFAULT_MAX_RETRIES = 5;

    @Inject
    KubernetesClient kubernetesClient;

    public <T extends HasMetadata> T create(T resource) {
        return createOrUpdate(resource, Command.CREATE);
    }

    public <T extends HasMetadata> T serverSideApply(T resource) {
        return createOrUpdate(resource, Command.SERVER_SIDE_APPLY);
    }

    public <T extends HasMetadata> T update(T resource) {
        return createOrUpdate(resource, Command.UPDATE);
    }

    private <T extends HasMetadata> T createOrUpdate(T resource, Command command) {
        RetryConfig config = RetryConfig.<T> custom()
                .maxAttempts(DEFAULT_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(DEFAULT_MAX_INTERVAL))
                .retryOnResult(r -> r == null)
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(command.toString());
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying {} for {}", command, resource.getMetadata().getName()));

        Function<T, T> decorated = Retry.decorateFunction(retry, (T r) -> {
            switch (command) {
                case CREATE:
                    return kubernetesClient.resource(r).create();
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

    public <T extends HasMetadata> List<StatusDetails> delete(Class<T> clazz, String namespace, String name) {
        RetryConfig config = RetryConfig.<List<StatusDetails>> custom()
                .maxAttempts(DEFAULT_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(DEFAULT_MAX_INTERVAL))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("delete");
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

    public <T extends HasMetadata> T get(Class<T> clazz, String namespace, String name) {
        RetryConfig config = RetryConfig.<T> custom()
                .maxAttempts(DEFAULT_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(DEFAULT_MAX_INTERVAL))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("get");
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

    public <T extends HasMetadata> List<T> list(Class<T> clazz, String namespace, ListOptions options) {
        RetryConfig config = RetryConfig.<List<T>> custom()
                .maxAttempts(DEFAULT_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(DEFAULT_MAX_INTERVAL))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("get");
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying list for {}", namespace));

        Supplier<List<T>> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.resources(clazz).inNamespace(namespace).list(options).getItems();
        });

        return decorated.get();
    }

    public List<HasMetadata> list(String namespace, ListOptions options) {
        RetryConfig config = RetryConfig.<List<HasMetadata>> custom()
                .maxAttempts(DEFAULT_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(DEFAULT_MAX_INTERVAL))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("get");
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying list for {}", namespace));

        Supplier<List<HasMetadata>> decorated = Retry.decorateSupplier(retry, () -> {
            return kubernetesClient.resources(HasMetadata.class).inNamespace(namespace).list(options).getItems();
        });

        return decorated.get();
    }

    public void waitForAllPodsRunning(String namespace) {
        RetryConfig config = RetryConfig.<Set<String>> custom()
                .maxAttempts(ALL_PODS_RUNNING_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(ALL_PODS_RUNNING_MAX_INTERVAL))
                .retryOnResult(phases -> phases.size() != 1 || !phases.contains(ALL_PODS_RUNNING_PHASE))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("pod-phasees");
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
            LOG.info("⚠️ WARN: All pods in the '{}' namespace are not yet {}", namespace, ALL_PODS_RUNNING_PHASE);
        }
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
}
