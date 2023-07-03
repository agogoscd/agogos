package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.Retries;
import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Priority
public abstract class Installer {
    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ResourceLoader resourceLoader;

    @Inject
    Retries retries;

    public abstract void install(InstallProfile profile, String namespace);
}
