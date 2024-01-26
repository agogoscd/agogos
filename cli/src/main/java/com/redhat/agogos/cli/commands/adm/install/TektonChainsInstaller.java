package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand;
import com.redhat.agogos.cli.commands.adm.utils.Utils;
import com.redhat.agogos.cli.config.TektonChainsDependency;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Profile(InstallCommand.InstallProfile.dev)
@Profile(InstallCommand.InstallProfile.local)
@Priority(10)
@ApplicationScoped
@RegisterForReflection
public class TektonChainsInstaller extends DependencyInstaller {

    private static final Logger LOG = LoggerFactory.getLogger(TektonInstaller.class);

    private static final String CONFIGMAP_CHAINS_CONFIG = "chains-config";

    private static final String SECRET_SIGNING_SECRETS = "signing-secrets";

    @Inject
    private TektonChainsDependency tektonChains;

    @Override
    public void install(InstallCommand.InstallProfile profile, String namespace) {
        helper.printStdout(String.format("🕞 Installing Tekton Chains %s...", tektonChains.version()));

        install(tektonChains, profile, namespace);

        configureFeatureFlags(profile, namespace);

        kubernetesFacade.waitForAllPodsRunning(tektonChains.namespace());

        helper.printStdout(String.format("✅ Tekton Chains %s installed", tektonChains.version()));
    }

    private void configureFeatureFlags(InstallCommand.InstallProfile profile, String namespace) {
        if (profile == InstallCommand.InstallProfile.dev || profile == InstallCommand.InstallProfile.local) {
            ConfigMap configMap = kubernetesFacade.get(ConfigMap.class, tektonChains.namespace(), CONFIGMAP_CHAINS_CONFIG);

            configMap = new ConfigMapBuilder(configMap)
                    .addToData("artifacts.oci.storage", "tekton")
                    .addToData("artifacts.taskrun.format", "in-toto")
                    .addToData("artifacts.taskrun.storage", "tekton")
                    .addToData("artifacts.taskrun.signer", "x509")
                    .build();

            kubernetesFacade.update(configMap);

            helper.printStdout(String.format("👉 OK: Configured Tekton Chains ConfigMap '%s'", CONFIGMAP_CHAINS_CONFIG));

            Secret signingSecret = kubernetesFacade.get(Secret.class, tektonChains.namespace(), SECRET_SIGNING_SECRETS);

            if (signingSecret != null) {
                helper.printStdout("🕞 Generating signing secret...");
                Map<String, String> envVars = new HashMap<>();
                envVars.put("COSIGN_PASSWORD", "not-ready-for-production");

                Integer statusCode = Utils.executeShellCommand("cosign generate-key-pair k8s://tekton-chains/signing-secrets",
                        (HashMap<String, String>) envVars);
                if (statusCode == 0)
                    helper.printStdout("👉 OK: Signing secret generated");
                else
                    helper.printStderr(String.format("⚠ Cosign error: status code %s", statusCode));
            }
        }
    }
}
