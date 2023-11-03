package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.config.DependencyConfig;
import com.redhat.agogos.core.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.io.IOException;
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
                resources.addAll(resourceLoader.loadResources(new URL(url).openStream()));
            } catch (IOException e) {
                throw new ApplicationException("Exception for URL " + url, e);
            }
        }

        if (consumer != null) {
            consumer.accept(resources);
        }

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata r : resources) {
            installed.add(kubernetesFacade.serverSideApply(r));
        }
        helper.printStatus(installed);
    }
}
