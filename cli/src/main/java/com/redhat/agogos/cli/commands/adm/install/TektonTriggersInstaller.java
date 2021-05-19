package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(15)
@ApplicationScoped
@RegisterForReflection
public class TektonTriggersInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(TektonTriggersInstaller.class);

    // In sync with https://docs.openshift.com/container-platform/4.7/cicd/pipelines/op-release-notes.html#getting-support
    private static final String VERSION = "v0.12.1";
    private static final String NAMESPACE = "tekton-pipelines";

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton Triggers {}...", VERSION);

        String url = String.format("https://storage.googleapis.com/tekton-releases/triggers/previous/%s/release.yaml",
                VERSION);

        status(installKubernetesResources(url, NAMESPACE));

        LOG.info("✅ Tekton Triggers {} installed", VERSION);
    }
}
