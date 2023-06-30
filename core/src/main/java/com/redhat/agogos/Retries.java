package com.redhat.agogos;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class Retries {

    private static final Logger LOG = LoggerFactory.getLogger(Retries.class);

    private static final String ALL_PODS_RUNNING_PHASE = "Running";
    private static final Integer ALL_PODS_RUNNING_MAX_INTERVAL = 5;
    private static final Integer ALL_PODS_RUNNING_MAX_RETRIES = 36;
    private static final Integer SERVER_SIDE_APPLY_MAX_INTERVAL = 2;
    private static final Integer SERVER_SIDE_APPLY_MAX_RETRIES = 2;

    @Inject
    KubernetesClient kubernetesClient;

    public Object serverSideApply(Resource<?> resource) {
        RetryConfig config = RetryConfig.<Boolean> custom()
                .maxAttempts(SERVER_SIDE_APPLY_MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(SERVER_SIDE_APPLY_MAX_INTERVAL))
                .retryOnResult(r -> r == null)
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("server-side-apply");
        retry.getEventPublisher()
                .onRetry(e -> LOG.warn("⚠️ WARN: Retrying serverSideApply for {}", resource.toString()));

        Function<Resource<?>, Object> decorated = Retry.decorateFunction(retry, (Resource<?> r) -> {
            return r.serverSideApply();
        });

        return decorated.apply(resource);
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
