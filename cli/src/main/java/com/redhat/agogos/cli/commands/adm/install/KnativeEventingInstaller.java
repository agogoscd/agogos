package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.config.KnativeEventingDependency;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(20)
@ApplicationScoped
@RegisterForReflection
public class KnativeEventingInstaller extends DependencyInstaller {

    @Inject
    KnativeEventingDependency eventing;

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.printStdout(String.format("🕞 Installing Knative Eventing %s...", eventing.version()));

        cleanup();
        install(eventing, profile, namespace, loaded -> {
            loaded.removeIf(
                    resource -> resource instanceof Deployment
                            && resource.getMetadata().getName().equals("pingsource-mt-adapter"));
        });

        kubernetesFacade.waitForAllPodsRunning(eventing.namespace());

        helper.printStdout(String.format("✅ Knative Eventing %s installed", eventing.version()));
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
        // These ClusterRoles get modified by the aggregation controller, and must be deleted before another install occurs.
        List<String> rolesToDelete = List.of("addressable-resolver", "channelable-manipulator", "podspecable-binding",
                "source-observer");

        helper.printStdout(String.format("🕞 Cleaning up Knative Eventing resources..."));

        kubernetesFacade.delete(Deployment.class, eventing.namespace(), "eventing-webhook");

        rolesToDelete.stream().forEach(role -> {
            kubernetesFacade.delete(ClusterRole.class, eventing.namespace(), role);
        });
    }
}
