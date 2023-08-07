package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(CRDsInstaller.class);

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos CRDs...");

        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("deployment/crds.yaml");

        List<HasMetadata> resources = resourceLoader.loadResources(stream);

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata resource : resources) {
            // Make sure we have the correct namespace.
            resource.getMetadata().setNamespace(namespace);
            installed.add(kubernetesFacade.serverSideApply(resource));
        }
        Helper.status(installed);

        LOG.info("✅ Agogos CRDs installed");
    }
}
