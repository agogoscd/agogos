package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.core.KubernetesFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Priority
public abstract class Installer {
    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    ResourceLoader resourceLoader;

    public abstract void install(InstallProfile profile, String namespace);
}
