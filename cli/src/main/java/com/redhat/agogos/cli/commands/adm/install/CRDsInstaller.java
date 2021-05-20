package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.InputStream;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                .getResourceAsStream("/deployment/crds.yaml");

        status(installKubernetesResources(stream, namespace));

        LOG.info("✅ Agogos CRDs installed");
    }
}
