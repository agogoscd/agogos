package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.core.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

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

        List<HasMetadata> resources = new ArrayList<>();
        try {
            ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
            for (Resource r : resourceResolver.getResources("classpath:deployment/crds/agogos*.yaml")) {
                resources.addAll(resourceLoader.loadResources(r.getInputStream()));
            }
        } catch (Exception e) {
            throw new ApplicationException("Unable to load CRDs from classpath", e);
        }

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata resource : resources) {
            installed.add(kubernetesFacade.serverSideApply(resource));
        }
        Helper.status(installed);

        LOG.info("✅ Agogos CRDs installed");
    }
}
