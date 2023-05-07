package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.config.TektonTriggersDependency;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(15)
@ApplicationScoped
@RegisterForReflection
public class TektonTriggersInstaller extends DependencyInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(TektonTriggersInstaller.class);

    @Inject
    TektonTriggersDependency triggers;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton Triggers {}...", triggers.version());

        install(triggers, profile, namespace);

        LOG.info("✅ Tekton Triggers {} installed", triggers.version());
    }
}
