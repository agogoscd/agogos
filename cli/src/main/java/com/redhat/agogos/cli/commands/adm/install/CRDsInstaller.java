package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import java.io.InputStream;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
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

        Helper.status(resourceLoader.installKubernetesResources(stream, namespace));

        LOG.info("✅ Agogos CRDs installed");
    }
}