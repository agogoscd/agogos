package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.config.KnativeEventingDependency;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(20)
@ApplicationScoped
@RegisterForReflection
public class KnativeEventingInstaller extends DependencyInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(KnativeEventingInstaller.class);

    @Inject
    KnativeEventingDependency eventing;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Knative Eventing {}...", eventing.version());

        cleanup();
        install(eventing, profile, namespace, loaded -> {
            loaded.removeIf(
                    resource -> resource instanceof Deployment
                            && resource.getMetadata().getName().equals("pingsource-mt-adapter"));
        });
        LOG.info("✅ Knative Eventing {} installed", eventing.version());
    }

    /**
     * <p>
     * Before installing Knative Eventing we need to make sure previous installation (if any) is in correct state.
     * The easiest way to achieve this is to remove webhooks. These will be recreated as part of the installation process later.
     * If we don't do this - Knative will restart webhooks and generate new certificates at the installation time.
     * This can cause failures in handling of Broker creation which will be performed after Knative Eventing installation is
     * done.
     * </p>
     * 
     */
    private void cleanup() {

        LOG.info("🕞 Cleaning up Knative Eventing resources...");

        String releaseLabel = "eventing.knative.dev/release";

        kubernetesClient.admissionRegistration().v1().validatingWebhookConfigurations()
                .withLabel(releaseLabel).delete();
        kubernetesClient.admissionRegistration().v1().mutatingWebhookConfigurations()
                .withLabel(releaseLabel)
                .delete();

        kubernetesClient.apps().deployments().inNamespace(eventing.namespace()).withName("eventing-webhook").delete();
    }
}
