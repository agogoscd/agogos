package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(15)
@ApplicationScoped
@RegisterForReflection
public class TektonTriggersInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(TektonTriggersInstaller.class);

    // In sync with https://docs.openshift.com/container-platform/4.7/cicd/pipelines/op-release-notes.html#getting-support
    private static final String VERSION = "v0.22.2";
    private static final String NAMESPACE = "tekton-pipelines";

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton Triggers {}...", VERSION);

        String[] files = new String[] { "tekton-triggers.yaml", "tekton-triggers-interceptors.yaml" };

        List<HasMetadata> resources = new ArrayList<>();

        for (String file : files) {
            String path = String.format("dependencies/%s", file);
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            resources.addAll(resourceLoader.installKubernetesResources(is, NAMESPACE));
        }

        Helper.status(resources);

        LOG.info("✅ Tekton Triggers {} installed", VERSION);
    }
}
