package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.errors.ApplicationException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

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

        try {
            ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
            for (Resource r : resourceResolver.getResources("classpath:deployment/crds/agogos*.yaml")) {
                Helper.status(resourceLoader.installKubernetesResources(r.getInputStream(), namespace));
            }
        } catch (Exception e) {
            throw new ApplicationException("Unable to load CRDs from classpath", e);
        }

        LOG.info("✅ Agogos CRDs installed");
    }
}
