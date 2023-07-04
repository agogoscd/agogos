package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.config.DependencyConfig;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class DependencyInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyInstaller.class);

    private static final String POD_RUNNING = "Running";
    private static final Integer MAX_RETRIES = 36;
    private static final Integer MAX_INTERVAL = 5;

    protected void install(DependencyConfig config, InstallProfile profile, String namespace) {
        install(config, profile, namespace, null);
    }

    protected void install(DependencyConfig config, InstallProfile profile, String namespace,
            Consumer<List<HasMetadata>> consumer) {
        List<HasMetadata> resources = new ArrayList<>();

        for (String url : config.urls()) {
            try {
                resources.addAll(resourceLoader.installKubernetesResources(new URL(url), config.namespace(), consumer));
            } catch (MalformedURLException e) {
                throw new ApplicationException("Malformed URL for " + config.namespace(), e);
            }
        }

        Helper.status(resources);
    }

    protected void waitForAllPodsRunning(String namespace) {
        RetryConfig config = RetryConfig.<Set<String>> custom()
                .maxAttempts(MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(MAX_INTERVAL))
                .retryOnResult(phases -> phases.size() != 1 || !phases.contains(POD_RUNNING))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("pod-phasees");
        retry.getEventPublisher()
                .onRetry(e -> LOG.info("⏳ WAIT: Waiting for all pods in the '{}' namespace to be {}", namespace, POD_RUNNING));
        Supplier<Set<String>> decorated = Retry.decorateSupplier(retry, () -> {
            return getAllPodPhases(namespace);
        });

        Set<String> result = (Set<String>) decorated.get();
        if (allPodsRunning(result)) {
            LOG.info("👉 OK: All pods in the '{}' namespace are {}", namespace, POD_RUNNING);
        } else {
            LOG.info("⚠️ WARN: All pods in the '{}' namespace are not yet {}", namespace, POD_RUNNING);
        }
    }

    private Set<String> getAllPodPhases(String namespace) {
        return kubernetesFacade
                .list(Pod.class, namespace)
                .stream()
                .map(p -> p.getStatus().getPhase())
                .collect(Collectors.toSet());
    }

    private boolean allPodsRunning(Set<String> phases) {
        return phases.size() == 1 && phases.contains(POD_RUNNING);
    }
}
