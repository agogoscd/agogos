package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.config.TektonPipelineDependency;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonInstaller extends DependencyInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(TektonInstaller.class);

    private static final String CONFIGMAP_CONFIG_DEFAULTS = "config-defaults";
    private static final String CONFIGMAP_FEATURE_FLAGS = "feature-flags";

    @Inject
    TektonPipelineDependency tekton;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Tekton {}...", tekton.version());

        install(tekton, profile, namespace);

        configureFeatureFlags(profile, namespace);

        kubernetesFacade.waitForAllPodsRunning(tekton.namespace());

        LOG.info("✅ Tekton {} installed", tekton.version());
    }

    private void configureFeatureFlags(InstallProfile profile, String namespace) {
        if (profile == InstallProfile.dev || profile == InstallProfile.local) {
            ConfigMap configMap = kubernetesFacade.get(ConfigMap.class, tekton.namespace(), CONFIGMAP_FEATURE_FLAGS);
            configMap = new ConfigMapBuilder(configMap)
                    .addToData("disable-affinity-assistant", "true")
                    .addToData("send-cloudevents-for-runs", "false")
                    .build();

            kubernetesFacade.serverSideApply(configMap);

            LOG.info("👉 OK: Configured Tekton ConfigMap '{}'", CONFIGMAP_FEATURE_FLAGS);
        }
    }
}
