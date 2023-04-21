package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(TektonInstaller.class);

    // In sync with https://docs.openshift.com/container-platform/4.7/cicd/pipelines/op-release-notes.html#getting-support
    private static final String VERSION = "v0.41.1";
    private static final String NAMESPACE = "tekton-pipelines";

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton {}...", VERSION);

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("dependencies/tekton.yaml");
        Helper.status(resourceLoader.installKubernetesResources(is, NAMESPACE));

        LOG.info("✅ Tekton {} installed", VERSION);
    }
}
