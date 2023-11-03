package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Profile(InstallProfile.prod)
@Priority(25)
@ApplicationScoped
@RegisterForReflection
public class CRDsInstaller extends Installer {

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.printStdout(String.format("🕞 Installing Agogos CRDs..."));

        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("deployment/crds.yaml");

        List<HasMetadata> resources = resourceLoader.loadResources(stream);

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata resource : resources) {

            installed.add(kubernetesFacade.serverSideApply(resource));
        }
        helper.printStatus(installed);

        helper.printStdout(String.format("✅ Agogos CRDs installed"));
    }
}
