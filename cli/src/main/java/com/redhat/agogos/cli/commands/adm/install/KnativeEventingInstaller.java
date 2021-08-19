package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(20)
@ApplicationScoped
@RegisterForReflection
public class KnativeEventingInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(KnativeEventingInstaller.class);

    // In sync with https://docs.openshift.com/container-platform/4.7/serverless/serverless-release-notes.html#serverless-rn-1-15-0_serverless-release-notes
    private static final String VERSION = "v0.21.4";
    private static final String NAMESPACE = "knative-eventing";

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Knative Eventing {}...", VERSION);

        cleanup();

        Helper.status(install());

        LOG.info("✅ Knative Eventing {} installed", VERSION);
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

        kubernetesClient.apps().deployments().inNamespace(NAMESPACE).withName("eventing-webhook").delete();
    }

    private List<HasMetadata> install() {
        List<HasMetadata> resources = new ArrayList<>();

        String[] files = new String[] { "core", "in-memory-channel", "mt-channel-broker" };

        for (String file : files) {
            String path = String.format("dependencies/knative-eventing-%s.yaml", file);
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

            resources.addAll(
                    resourceLoader.installKubernetesResources(
                            is,
                            NAMESPACE,
                            loaded -> {
                                loaded.removeIf(
                                        resource -> resource instanceof Deployment
                                                && resource.getMetadata().getName().equals("pingsource-mt-adapter"));
                            }));
        }

        return resources;

    }
}
