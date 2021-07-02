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
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(TektonInstaller.class);

    // In sync with https://docs.openshift.com/container-platform/4.7/cicd/pipelines/op-release-notes.html#getting-support
    private static final String VERSION = "v0.22.0";
    private static final String NAMESPACE = "tekton-pipelines";

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton {}...", VERSION);

        String path = String.format("dependencies/tekton-%s.yaml", VERSION);
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        Helper.status(resourceLoader.installKubernetesResources(is, NAMESPACE));

        LOG.info("✅ Tekton {} installed", VERSION);
    }
}
