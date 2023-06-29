package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.config.TektonPipelineDependency;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonInstaller extends DependencyInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(TektonInstaller.class);

    @ConfigProperty(name = "agogos.cloud-events.base-url", defaultValue = "http://broker-ingress.knative-eventing.svc.cluster.local")
    String baseUrl;

    @Inject
    TektonPipelineDependency tekton;

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton {}...", tekton.version());

        install(tekton, profile, namespace);

        // configureForCloudEvents(namespace);

        waitForAllPodsRunning(tekton.namespace());

        LOG.info("✅ Tekton {} installed", tekton.version());
    }

    private void configureForCloudEvents(String namespace) {
        kubernetesClient.configMaps()
                .inNamespace(tekton.namespace())
                .withName(tekton.configmap())
                .edit(c -> new ConfigMapBuilder(c)
                        .addToData("default-cloud-events-sink", String.format("%s/%s/agogos", baseUrl, namespace))
                        .addToData("send-cloudevents-for-runs", "true")
                        .build());

        LOG.info("👉 OK: Configured Tekton ConfigMap '{}' for cloud events", tekton.configmap());
    }
}
