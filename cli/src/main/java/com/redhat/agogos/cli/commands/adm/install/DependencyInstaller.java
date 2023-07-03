package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.config.DependencyConfig;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class DependencyInstaller extends Installer {

    protected void install(DependencyConfig config, InstallProfile profile, String namespace) {
        install(config, profile, namespace, null);
    }

    protected void install(DependencyConfig config, InstallProfile profile, String namespace,
            Consumer<List<HasMetadata>> consumer) {
        List<HasMetadata> resources = new ArrayList<>();

        for (String url : config.urls()) {
            try {
                resources.addAll(resourceLoader.installKubernetesResources(new URL(url), config.namespace(), consumer));
            } catch (MalformedURLException e) {
                throw new ApplicationException("Malformed URL for " + config.namespace(), e);
            }
        }

        Helper.status(resources);
    }
}
