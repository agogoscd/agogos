package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.config.TektonPipelineDependency;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonInstaller extends DependencyInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(TektonInstaller.class);

    @Inject
    TektonPipelineDependency tekton;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton {}...", tekton.version());

        install(tekton, profile, namespace);

        LOG.info("✅ Tekton {} installed", tekton.version());
    }
}
