package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.config.TektonTriggersDependency;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(15)
@ApplicationScoped
@RegisterForReflection
public class TektonTriggersInstaller extends DependencyInstaller {

    @Inject
    TektonTriggersDependency triggers;

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.printStdout(String.format("🕞 Installing Tekton Triggers %s...", triggers.version()));

        install(triggers, profile, namespace);

        kubernetesFacade.waitForAllPodsRunning(triggers.namespace());

        helper.printStdout(String.format("✅ Tekton Triggers %s installed", triggers.version()));
    }
}
