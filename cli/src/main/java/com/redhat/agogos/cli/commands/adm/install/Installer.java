package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.client.KubernetesClient;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Priority
public abstract class Installer {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ResourceLoader resourceLoader;

    public abstract void install(InstallProfile profile, String namespace);
}
