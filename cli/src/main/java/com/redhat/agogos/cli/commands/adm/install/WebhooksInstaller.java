package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.commands.adm.certs.CertProvider;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Stage;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperations;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperationsBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Profile(InstallProfile.prod)
@Priority(95)
@ApplicationScoped
@RegisterForReflection
public class WebhooksInstaller extends Installer {

    @ConfigProperty(name = "agogos.webhooks.container-image")
    private String ContainerImage;

    @ConfigProperty(name = "agogos.webhooks.service-account")
    private String ServiceAccountName;

    @Inject
    CertProvider certProvider;

    private static final Map<String, String> LABELS = Map.of(
            "app.kubernetes.io/part-of", "agogos",
            "app.kubernetes.io/component", "webhooks");

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.printStdout(String.format("🕞 Installing Agogos Webhooks component..."));

        certProvider.init();

        List<HasMetadata> resources = new ArrayList<>();

        // Default resources for any profile selected
        resources.addAll(
                List.of(
                        admissionValidation(profile, namespace),
                        admissionMutation(profile, namespace),
                        serviceAccount(namespace),
                        clusterRole(),
                        clusterRoleBinding(namespace),
                        secret(namespace)));

        // For local and prod profiles we need to add more resources
        if (profile == InstallProfile.local || profile == InstallProfile.prod) {
            resources.addAll(
                    List.of(
                            service(namespace),
                            deployment(namespace)));
        }

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata r : resources) {
            installed.add(kubernetesFacade.serverSideApply(r));
        }

        if (profile == InstallProfile.local || profile == InstallProfile.prod) {
            Service webhooksService = kubernetesFacade.get(Service.class, namespace, ServiceAccountName);
            if (webhooksService != null) {
                helper.printStdout(String.format("🕞 Restarting Webhooks service after updating certificates..."));

                kubernetesFacade.restartDeployment(namespace, ServiceAccountName);
            }
        }

        helper.printStatus(installed);

        helper.printStdout(String.format("✅ Agogos Webhooks installed"));

        if (profile == InstallProfile.dev) {
            writeCerts();
        }
    }

    private void writeCerts() {
        Path keyPath = Paths.get("webhooks.pem");
        Path certPath = Paths.get("webhooks.crt");
        Path pathBase = Paths.get(".");
        Path keyPathRelative = pathBase.relativize(keyPath);
        Path certPathRelative = pathBase.relativize(certPath);

        try {
            Files.writeString(keyPath, certProvider.privateKey());
        } catch (IOException e) {
            throw new ApplicationException("Could not write private key to file '{}'", keyPath.toAbsolutePath());
        }

        try {
            Files.writeString(certPath, certProvider.certificate());
        } catch (IOException e) {
            throw new ApplicationException("Could not write certificate to file '{}'", certPath.toAbsolutePath());
        }

        helper.printStdout(
                "\n👋 Webhook configuration in development mode. You can use following environment variables to point the Webhook application to generated certificate:\n");
        helper.printStdout(String.format("👉 QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES=./%s", keyPathRelative));
        helper.printStdout(String.format("👉 QUARKUS_HTTP_SSL_CERTIFICATE_FILES=./%s", certPathRelative));
    }

    private Deployment deployment(String namespace) {
        Probe livenessProbe = new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder().withPath("/").withPort(new IntOrString(7080)).build())
                .withInitialDelaySeconds(30).withPeriodSeconds(3).build();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", Quantity.parse("256Mi"));
        requests.put("cpu", Quantity.parse("400m"));

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("memory", Quantity.parse("512Mi"));
        limits.put("cpu", Quantity.parse("800m"));

        Container webhookContainer = new ContainerBuilder()
                .withName("webhooks").withImage(ContainerImage).withImagePullPolicy("Always")
                .withEnv(
                        new EnvVarBuilder().withName("NAMESPACE").withValue(namespace).build(),
                        new EnvVarBuilder().withName("QUARKUS_HTTP_SSL_CERTIFICATE_FILES").withValue("/certs/tls.crt").build(),
                        new EnvVarBuilder().withName("QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES").withValue("/certs/tls.key")
                                .build())
                .withVolumeMounts(new VolumeMountBuilder().withName("certs").withMountPath("/certs").withReadOnly(true).build())
                .withPorts(
                        new ContainerPortBuilder().withName("http").withContainerPort(7080).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName("https").withContainerPort(8443).withProtocol("TCP").build())
                .withLivenessProbe(livenessProbe)
                .withNewResources()
                .withRequests(requests).withLimits(limits)
                .endResources()
                .build();

        Deployment webhookDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(ServiceAccountName)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .withMatchLabels(LABELS)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                .withContainers(webhookContainer)
                .withVolumes(new VolumeBuilder().withName("certs")
                        .withSecret(new SecretVolumeSourceBuilder().withSecretName(ServiceAccountName).build())
                        .build())
                .withServiceAccount(ServiceAccountName)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        return webhookDeployment;
    }

    private Service service(String namespace) {
        ServicePort httpPort = new ServicePortBuilder()
                .withName("http")
                .withPort(80)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(7080))
                .build();

        ServicePort httpsPort = new ServicePortBuilder()
                .withName("https")
                .withPort(443)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(8443))
                .build();

        Service webhookService = new ServiceBuilder()
                .withNewMetadata()
                .withName(ServiceAccountName)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                .withPorts(httpPort, httpsPort)
                .withType("ClusterIP")
                .withSelector(LABELS)
                .endSpec()
                .build();

        return webhookService;
    }

    private ValidatingWebhookConfiguration admissionValidation(InstallProfile profile, String namespace) {
        RuleWithOperations validationRules = new RuleWithOperationsBuilder()
                .withOperations("CREATE", "UPDATE")
                .withApiGroups("agogos.redhat.com")
                .withApiVersions("v1alpha1")
                .withResources(HasMetadata.getPlural(Build.class),
                        HasMetadata.getPlural(Component.class),
                        HasMetadata.getPlural(com.redhat.agogos.core.v1alpha1.Pipeline.class),
                        HasMetadata.getPlural(Stage.class))
                .withScope("*")
                .build();

        ValidatingWebhookBuilder validatingWebhookBuilder = new ValidatingWebhookBuilder()
                .withName("validate.webhook.agogos.redhat.com")
                .withSideEffects("None")
                .withFailurePolicy("Fail")
                .withAdmissionReviewVersions("v1")
                .withRules(validationRules);

        switch (profile) {
            case dev:
                validatingWebhookBuilder
                        .withNewClientConfig()
                        .withUrl("https://host.minikube.internal:8443/webhooks/validate")
                        .withCaBundle(certProvider.caBundle())
                        .endClientConfig();
                break;
            case local:
            case prod:
                validatingWebhookBuilder
                        .withNewClientConfig()
                        .withNewService()
                        .withNamespace(namespace)
                        .withName(ServiceAccountName)
                        .withPath("/webhooks/validate")
                        .withPort(443)
                        .endService()
                        .withCaBundle(certProvider.caBundle())
                        .endClientConfig();
                break;
            default:
                break;
        }

        ValidatingWebhookConfiguration validatingWebhookConfiguration = new ValidatingWebhookConfigurationBuilder()
                .withNewMetadata()
                .withName("webhook.agogos.redhat.com")
                .withLabels(LABELS)
                .endMetadata()
                .withWebhooks(validatingWebhookBuilder.build())
                .build();

        return validatingWebhookConfiguration;
    }

    private MutatingWebhookConfiguration admissionMutation(InstallProfile profile, String namespace) {
        RuleWithOperations mutationRules = new RuleWithOperationsBuilder().withOperations("CREATE")
                .withApiGroups("agogos.redhat.com").withApiVersions("v1alpha1").withResources("builds", "runs")
                .withScope("*").build();

        MutatingWebhookBuilder mutatingWebhookBuilder = new MutatingWebhookBuilder()
                .withName("mutate.webhook.agogos.redhat.com")
                .withSideEffects("None")
                .withFailurePolicy("Fail")
                .withAdmissionReviewVersions("v1")
                .withRules(mutationRules);

        switch (profile) {
            case dev:
                mutatingWebhookBuilder
                        .withNewClientConfig()
                        .withUrl("https://host.minikube.internal:8443/webhooks/mutate")
                        .withCaBundle(certProvider.caBundle())
                        .endClientConfig();
                break;
            case local:
            case prod:
                mutatingWebhookBuilder
                        .withNewClientConfig()
                        .withNewService()
                        .withNamespace(namespace)
                        .withName(ServiceAccountName)
                        .withPath("/webhooks/mutate")
                        .withPort(443)
                        .endService()
                        .withCaBundle(certProvider.caBundle())
                        .endClientConfig();
                break;
            default:
                break;
        }

        MutatingWebhookConfiguration mutatingWebhookConfiguration = new MutatingWebhookConfigurationBuilder()
                .withNewMetadata().withName("webhook.agogos.redhat.com").withLabels(LABELS).endMetadata()
                .withWebhooks(mutatingWebhookBuilder.build())
                .build();

        return mutatingWebhookConfiguration;
    }

    private Secret secret(String namespace) {
        Map<String, String> certData = new HashMap<>();
        certData.put("tls.crt", CertProvider.toBase64(certProvider.certificate()));
        certData.put("tls.key", CertProvider.toBase64(certProvider.privateKey()));

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(ServiceAccountName)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .withType("kubernetes.io/tls")
                .withData(certData)
                .build();

        return secret;
    }

    private ServiceAccount serviceAccount(String namespace) {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(ServiceAccountName)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .build();

        return sa;
    }

    private ClusterRole clusterRole() {
        ClusterRole cr = new ClusterRoleBuilder().withNewMetadata().withName(ServiceAccountName).withLabels(LABELS)
                .endMetadata().withRules(
                        new PolicyRuleBuilder().withApiGroups("agogos.redhat.com")
                                .withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build(),
                        new PolicyRuleBuilder().withApiGroups("tekton.dev")
                                .withResources("*")
                                .withVerbs("get", "list", "watch")
                                .build())
                .build();

        return cr;
    }

    private ClusterRoleBinding clusterRoleBinding(String namespace) {

        ClusterRoleBinding crb = new ClusterRoleBindingBuilder().withNewMetadata().withName(ServiceAccountName)
                .withLabels(LABELS).endMetadata()
                .withSubjects(
                        new SubjectBuilder().withKind("ServiceAccount").withName(ServiceAccountName).withNamespace(namespace)
                                .build())
                .withNewRoleRef().withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .withKind(HasMetadata.getKind(ClusterRole.class))
                .withName(ServiceAccountName)
                .endRoleRef().build();

        return crb;
    }
}
