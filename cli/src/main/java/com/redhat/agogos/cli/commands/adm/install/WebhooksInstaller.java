package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.commands.adm.certs.CertProvider;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Handler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
@Priority(95)
@ApplicationScoped
@RegisterForReflection
public class WebhooksInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(WebhooksInstaller.class);

    private static final String NAME = "agogos-webhooks";
    private static final String CONTAINER_IMAGE = "quay.io/cpaas/agogos-poc-webhooks:devel";

    @Inject
    CertProvider certProvider;

    private static final Map<String, String> LABELS = Map.of(//
            "app.kubernetes.io/instance", "default", //
            "app.kubernetes.io/part-of", "agogos", //
            "app.kubernetes.io/component", "webhooks"//
    );

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos Webhook component...");

        certProvider.init();

        List<HasMetadata> resources = new ArrayList<>();

        // Default resources for any profile selected
        resources.addAll(//
                List.of( //
                        admissionValidation(profile, namespace),
                        admissionMutation(profile, namespace) //
                ));

        // For local profile we need to add more resources
        if (profile == InstallProfile.local) {
            resources.addAll( //
                    List.of( //
                            serviceAccount(),
                            clusterRole(),
                            clusterRoleBinding(namespace),
                            secret(),
                            service(),
                            deployment() //
                    ));
        }

        Service webhooksService = kubernetesClient.services().inNamespace(namespace).withName(NAME).get();

        resources = resourceLoader.installKubernetesResources(resources, namespace);

        if (webhooksService != null && profile == InstallProfile.local) {
            LOG.info("🕞 Restarting Webhooks service after updating certificates...");

            kubernetesClient.apps().deployments().inNamespace(namespace).withName(NAME).rolling().restart();
        }

        Helper.status(resources);

        LOG.info("✅ Agogos Webhook installed");

        if (profile == InstallProfile.dev) {
            writeCerts();
        }
    }

    private void writeCerts() {
        Path keyPath = Paths.get("key.pem");
        Path certPath = Paths.get("webhooks.crt");

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

        LOG.info(
                "\n👋 Webhook configuration in development mode. You can use following environment variables to point the Webhook application to generated certificate:\n");
        LOG.info("👉 QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILE={}", keyPath.toAbsolutePath());
        LOG.info("👉 QUARKUS_HTTP_SSL_CERTIFICATE_FILE={}", certPath.toAbsolutePath());
    }

    private Deployment deployment() {
        Probe livenessProbe = new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder().withPath("/").withPort(new IntOrString(7080)).build())
                .withInitialDelaySeconds(3).withPeriodSeconds(3).build();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", Quantity.parse("256Mi"));
        requests.put("cpu", Quantity.parse("400m"));

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("memory", Quantity.parse("512Mi"));
        limits.put("cpu", Quantity.parse("800m"));

        Container webhookContainer = new ContainerBuilder()
                .withName("webhooks").withImage(CONTAINER_IMAGE).withImagePullPolicy("Always") //
                .withEnv(
                        new EnvVarBuilder().withName("QUARKUS_HTTP_SSL_CERTIFICATE_FILE").withValue("/certs/tls.crt").build(), //
                        new EnvVarBuilder().withName("QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILE").withValue("/certs/tls.key")
                                .build()) //
                .withVolumeMounts(new VolumeMountBuilder().withName("certs").withMountPath("/certs").withReadOnly(true).build()) //
                .withPorts(//
                        new ContainerPortBuilder().withName("http").withContainerPort(7080).withProtocol("TCP").build(), //
                        new ContainerPortBuilder().withName("https").withContainerPort(8443).withProtocol("TCP").build() //
                )//
                .withLivenessProbe(livenessProbe) //
                .withNewResources() //
                .withRequests(requests).withLimits(limits) //
                .endResources() //
                .build();

        Deployment webhookDeployment = new DeploymentBuilder().withNewMetadata().withName(NAME)
                .withLabels(LABELS)
                .endMetadata().withNewSpec().withReplicas(1).withNewSelector().withMatchLabels(LABELS).endSelector()
                .withNewTemplate().withNewMetadata().withLabels(LABELS).endMetadata().withNewSpec()
                .withContainers(webhookContainer)
                .withVolumes(new VolumeBuilder().withName("certs")
                        .withSecret(new SecretVolumeSourceBuilder().withSecretName(NAME).build())
                        .build())
                .withServiceAccount(NAME)
                .endSpec().endTemplate().endSpec().build();

        return webhookDeployment;
    }

    private Service service() {
        ServicePort httpPort = new ServicePortBuilder().withName("http").withPort(80).withProtocol("TCP")
                .withTargetPort(new IntOrString(7080)).build();
        ServicePort httpsPort = new ServicePortBuilder().withName("https").withPort(443).withProtocol("TCP")
                .withTargetPort(new IntOrString(8443)).build();

        Service webhookService = new ServiceBuilder().withNewMetadata().withName(NAME).withLabels(LABELS)
                .endMetadata().withNewSpec().withPorts(httpPort, httpsPort).withType("ClusterIP").withSelector(LABELS)
                .endSpec()
                .build();

        return webhookService;
    }

    private ValidatingWebhookConfiguration admissionValidation(InstallProfile profile, String namespace) {
        RuleWithOperations validationRules = new RuleWithOperationsBuilder() //
                .withOperations("CREATE", "UPDATE")
                .withApiGroups("agogos.redhat.com") //
                .withApiVersions("v1alpha1")//
                .withResources(HasMetadata.getPlural(Build.class), HasMetadata.getPlural(Component.class),
                        HasMetadata.getPlural(Handler.class)) //
                .withScope("*") //
                .build();

        ValidatingWebhookBuilder validatingWebhookBuilder = new ValidatingWebhookBuilder() //
                .withName("validate.webhook.agogos.redhat.com") //
                .withSideEffects("None") //
                .withFailurePolicy("Fail") //
                .withAdmissionReviewVersions("v1")//
                .withRules(validationRules); //

        switch (profile) {
            case local:
                validatingWebhookBuilder
                        .withNewClientConfig() //
                        .withNewService() //
                        .withNamespace(namespace) //
                        .withName(NAME) //
                        .withPath("/webhooks/validate") //
                        .withPort(443)//
                        .endService()//
                        .withCaBundle(certProvider.caBundle())//
                        .endClientConfig(); //
                break;
            case dev:
                validatingWebhookBuilder
                        .withNewClientConfig() //
                        .withUrl("https://192.168.39.1:8443/webhooks/validate")
                        .withCaBundle(certProvider.caBundle())//
                        .endClientConfig(); //
            default:
                break;
        }

        ValidatingWebhookConfiguration validatingWebhookConfiguration = new ValidatingWebhookConfigurationBuilder()
                .withNewMetadata() //
                .withName("webhook.agogos.redhat.com") //
                .withLabels(LABELS)//
                .endMetadata() //
                .withWebhooks(validatingWebhookBuilder.build()) //
                .build();

        return validatingWebhookConfiguration;
    }

    private MutatingWebhookConfiguration admissionMutation(InstallProfile profile, String namespace) {
        RuleWithOperations mutationRules = new RuleWithOperationsBuilder().withOperations("CREATE")
                .withApiGroups("agogos.redhat.com").withApiVersions("v1alpha1").withResources("builds", "runs")
                .withScope("*").build();

        MutatingWebhookBuilder mutatingWebhookBuilder = new MutatingWebhookBuilder() //
                .withName("mutate.webhook.agogos.redhat.com") //
                .withSideEffects("None") //
                .withFailurePolicy("Fail") //
                .withAdmissionReviewVersions("v1") //
                .withRules(mutationRules);

        switch (profile) {
            case local:
                mutatingWebhookBuilder
                        .withNewClientConfig()//
                        .withNewService() //
                        .withNamespace(namespace) //
                        .withName(NAME) //
                        .withPath("/webhooks/mutate") //
                        .withPort(443) //
                        .endService() //
                        .withCaBundle(certProvider.caBundle()) //
                        .endClientConfig();
                break;
            case dev:
                mutatingWebhookBuilder
                        .withNewClientConfig() //
                        .withUrl("https://192.168.39.1:8443/webhooks/mutate")
                        .withCaBundle(certProvider.caBundle())//
                        .endClientConfig(); //
            default:
                break;
        }

        MutatingWebhookConfiguration mutatingWebhookConfiguration = new MutatingWebhookConfigurationBuilder()
                .withNewMetadata().withName("webhook.agogos.redhat.com").withLabels(LABELS).endMetadata()
                .withWebhooks(mutatingWebhookBuilder.build())
                .build();

        return mutatingWebhookConfiguration;
    }

    private Secret secret() {
        Map<String, String> certData = new HashMap<>();
        certData.put("tls.crt", CertProvider.toBase64(certProvider.certificate()));
        certData.put("tls.key", CertProvider.toBase64(certProvider.privateKey()));

        Secret secret = new SecretBuilder().withNewMetadata().withName(NAME).withLabels(LABELS)
                .endMetadata()
                .withType("kubernetes.io/tls").withData(certData).build();

        return secret;
    }

    private ServiceAccount serviceAccount() {
        ServiceAccount sa = new ServiceAccountBuilder().withNewMetadata().withName(NAME)
                .withLabels(LABELS).endMetadata()
                .build();

        return sa;
    }

    private ClusterRole clusterRole() {
        ClusterRole cr = new ClusterRoleBuilder().withNewMetadata().withName(NAME).withLabels(LABELS)
                .endMetadata().withRules( //
                        new PolicyRuleBuilder().withApiGroups("agogos.redhat.com")
                                .withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build() //
                ).build();

        return cr;
    }

    private ClusterRoleBinding clusterRoleBinding(String namespace) {

        ClusterRoleBinding crb = new ClusterRoleBindingBuilder().withNewMetadata().withName(NAME)
                .withLabels(LABELS).endMetadata()
                .withSubjects(
                        new SubjectBuilder().withKind("ServiceAccount").withName(NAME).withNamespace(namespace)
                                .build())
                .withNewRoleRef().withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .withKind(HasMetadata.getKind(ClusterRole.class))
                .withName(NAME)
                .endRoleRef().build();

        return crb;
    }
}
