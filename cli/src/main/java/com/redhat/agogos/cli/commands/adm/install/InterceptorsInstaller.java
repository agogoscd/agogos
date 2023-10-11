package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.cli.commands.adm.certs.CertProvider;
import com.redhat.agogos.core.errors.ApplicationException;
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
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.tekton.triggers.v1alpha1.ClientConfig;
import io.fabric8.tekton.triggers.v1alpha1.ClientConfigBuilder;
import io.fabric8.tekton.triggers.v1alpha1.ClusterInterceptor;
import io.fabric8.tekton.triggers.v1alpha1.ClusterInterceptorBuilder;
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
import java.util.stream.Collectors;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Profile(InstallProfile.prod)
@Priority(95)
@ApplicationScoped
@RegisterForReflection
public class InterceptorsInstaller extends Installer {

    @ConfigProperty(name = "agogos.interceptors.container-image")
    private String ContainerImage;

    @ConfigProperty(name = "agogos.interceptors.service-account")
    private String ServiceAccountName;

    @ConfigProperty(name = "agogos.interceptors.url")
    private String Url;

    @ConfigProperty(name = "agogos.interceptors.port")
    private Integer Port;

    @ConfigProperty(name = "agogos.interceptors.endpoints")
    private List<String> Endpoints;

    @Inject
    CertProvider certProvider;

    private static final Map<String, String> LABELS = Map.of(
            "app.kubernetes.io/part-of", "agogos",
            "app.kubernetes.io/component", "interceptors");

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.println(String.format("🕞 Installing Agogos Interceptors component..."));

        certProvider.init();

        List<HasMetadata> resources = new ArrayList<>();
        resources.add(serviceAccount(namespace));
        resources.add(secret(namespace));

        if (profile == InstallProfile.dev) {
            resources.addAll(clusterInterceptors());
        } else {
            // For local and prod profiles we add services and deployments, as interceptors
            // run in the cluster, not in localhost
            Service service = service(namespace);
            resources.add(service);
            resources.addAll(clusterInterceptors(service));
            resources.add(deployment(namespace));
        }

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata r : resources) {
            installed.add(kubernetesFacade.serverSideApply(r));
        }

        if (profile == InstallProfile.local || profile == InstallProfile.prod) {
            Service interceptorsService = kubernetesFacade.get(Service.class, namespace, ServiceAccountName);
            if (interceptorsService != null) {
                helper.println(String.format("🕞 Restarting Interceptors service after updating certificates..."));

                kubernetesFacade.getKubernetesClient().apps().deployments().inNamespace(namespace).withName(ServiceAccountName)
                        .rolling().restart();
            }
        }

        helper.status(installed);

        helper.println(String.format("✅ Agogos Interceptors installed"));

        if (profile == InstallProfile.dev) {
            writeCerts();
        }
    }

    private void writeCerts() {
        Path keyPath = Paths.get("interceptors.pem");
        Path certPath = Paths.get("interceptors.crt");

        try {
            Files.writeString(keyPath, certProvider.privateKey());
        } catch (IOException e) {
            throw new ApplicationException("Could not write private key to file '%s'", keyPath.toAbsolutePath());
        }

        try {
            Files.writeString(certPath, certProvider.certificate());
        } catch (IOException e) {
            throw new ApplicationException("Could not write certificate to file '%s'", certPath.toAbsolutePath());
        }

        helper.println(
                "\n👋 Interceptor configuration in development mode. You can use following environment variables to point the Interceptor application to generated certificate:\n");
        helper.println(String.format("👉 QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES=%s", keyPath.toAbsolutePath()));
        helper.println(String.format("👉 QUARKUS_HTTP_SSL_CERTIFICATE_FILES=%s", certPath.toAbsolutePath()));
    }

    private Deployment deployment(String namespace) {
        Probe livenessProbe = new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder().withPath("/").withPort(new IntOrString(7090)).build())
                .withInitialDelaySeconds(30).withPeriodSeconds(3).build();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", Quantity.parse("256Mi"));
        requests.put("cpu", Quantity.parse("400m"));

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("memory", Quantity.parse("512Mi"));
        limits.put("cpu", Quantity.parse("800m"));

        Container interceptorContainer = new ContainerBuilder()
                .withName("interceptors").withImage(ContainerImage).withImagePullPolicy("Always")
                .withEnv(
                        new EnvVarBuilder().withName("QUARKUS_HTTP_SSL_CERTIFICATE_FILES").withValue("/certs/tls.crt").build(),
                        new EnvVarBuilder().withName("QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES").withValue("/certs/tls.key")
                                .build())
                .withVolumeMounts(new VolumeMountBuilder().withName("certs").withMountPath("/certs").withReadOnly(true).build())
                .withPorts(
                        new ContainerPortBuilder().withName("http").withContainerPort(7090).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withName("https").withContainerPort(8443).withProtocol("TCP").build())
                .withLivenessProbe(livenessProbe)
                .withNewResources()
                .withRequests(requests).withLimits(limits)
                .endResources()
                .withEnv(new EnvVarBuilder().withName("NAMESPACE").withValue(namespace).build())
                .build();

        Deployment interceptorDeployment = new DeploymentBuilder()
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
                .withContainers(interceptorContainer)
                .withVolumes(new VolumeBuilder().withName("certs")
                        .withSecret(new SecretVolumeSourceBuilder().withSecretName(ServiceAccountName).build())
                        .build())
                .withServiceAccount(ServiceAccountName)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        return interceptorDeployment;
    }

    private Service service(String namespace) {
        ServicePort httpPort = new ServicePortBuilder()
                .withName("http")
                .withPort(80)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(7090))
                .build();

        ServicePort httpsPort = new ServicePortBuilder()
                .withName("https")
                .withPort(443)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(8443))
                .build();

        Service interceptorService = new ServiceBuilder()
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

        return interceptorService;
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

    private List<ClusterInterceptor> clusterInterceptors() {
        return Endpoints.stream()
                .map(e -> clusterInterceptor(e,
                        new ClientConfigBuilder()
                                .withUrl(String.format("%s/%s", Url, e))
                                .build()))
                .collect(Collectors.toList());
    }

    private List<ClusterInterceptor> clusterInterceptors(Service service) {
        return Endpoints.stream()
                .map(e -> clusterInterceptor(e,
                        new ClientConfigBuilder()
                                .withNewService()
                                .withName(service.getMetadata().getName())
                                .withNamespace(service.getMetadata().getNamespace())
                                .withPath(e)
                                .withPort(Port)
                                .endService()
                                .build()))
                .collect(Collectors.toList());
    }

    private ClusterInterceptor clusterInterceptor(String name, ClientConfig config) {

        ClusterInterceptor ci = new ClusterInterceptorBuilder()
                .withKind(HasMetadata.getKind(ClusterInterceptor.class))
                .withNewMetadata()
                .withName(name)
                .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                .withClientConfig(config)
                .endSpec()
                .build();

        return ci;
    }
}
