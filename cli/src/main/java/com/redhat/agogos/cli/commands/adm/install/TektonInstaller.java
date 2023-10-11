package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.config.TektonPipelineDependency;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonInstaller extends DependencyInstaller {

    private static final String CONFIGMAP_FEATURE_FLAGS = "feature-flags";

    @Inject
    TektonPipelineDependency tekton;

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.println(String.format("🕞 Installing Tekton %s...", tekton.version()));

        install(tekton, profile, namespace);

        configureFeatureFlags(profile, namespace);

        kubernetesFacade.waitForAllPodsRunning(tekton.namespace());

        helper.println(String.format("✅ Tekton %s installed", tekton.version()));
    }

    private void configureFeatureFlags(InstallProfile profile, String namespace) {
        if (profile == InstallProfile.dev || profile == InstallProfile.local) {
            ConfigMap configMap = kubernetesFacade.get(ConfigMap.class, tekton.namespace(), CONFIGMAP_FEATURE_FLAGS);
            configMap = new ConfigMapBuilder(configMap)
                    .addToData("disable-affinity-assistant", "true")
                    .addToData("send-cloudevents-for-runs", "false")
                    .build();

            kubernetesFacade.serverSideApply(configMap);

            helper.println(String.format("👉 OK: Configured Tekton ConfigMap '%s'", CONFIGMAP_FEATURE_FLAGS));
        }
    }
}
