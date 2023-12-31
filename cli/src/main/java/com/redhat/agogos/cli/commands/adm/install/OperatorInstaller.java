package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
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
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Profile(InstallProfile.prod)
@Priority(99)
@ApplicationScoped
@RegisterForReflection
public class OperatorInstaller extends Installer {

    @ConfigProperty(name = "agogos.operator.container-image")
    private String ContainerImage;

    @ConfigProperty(name = "agogos.operator.service-account")
    private String ServiceAccountName;

    private static final Map<String, String> LABELS = Map.of(
            "app.kubernetes.io/part-of", "agogos",
            "app.kubernetes.io/component", "operator");

    @Override
    public void install(InstallProfile profile, String namespace) {
        helper.printStdout(String.format("🕞 Installing Agogos Operator component..."));

        List<HasMetadata> resources = Stream.of(serviceAccount(namespace), clusterRole(), clusterRoleBinding(namespace))
                .collect(Collectors.toList());

        // For local and prod profiles we need to add more resources
        if (profile == InstallProfile.local || profile == InstallProfile.prod) {
            resources.addAll(
                    List.of(service(namespace),
                            deployment(namespace)));
        }

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata r : resources) {
            installed.add(kubernetesFacade.serverSideApply(r));
        }

        helper.printStatus(installed);

        helper.printStdout(String.format("✅ Agogos Operator installed"));
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
                        new PolicyRuleBuilder().withApiGroups("apiextensions.k8s.io").withResources("customresourcedefinitions")
                                .withVerbs("get", "list", "watch").build(),
                        new PolicyRuleBuilder().withApiGroups("tekton.dev").withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build(),
                        new PolicyRuleBuilder().withApiGroups("triggers.tekton.dev").withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build())
                .build();

        return cr;
    }

    private ClusterRoleBinding clusterRoleBinding(String namespace) {

        ClusterRoleBinding crb = new ClusterRoleBindingBuilder().withNewMetadata().withName(ServiceAccountName)
                .withLabels(LABELS).endMetadata()
                .withSubjects(
                        new SubjectBuilder().withKind(HasMetadata.getKind(ServiceAccount.class)).withName(ServiceAccountName)
                                .withNamespace(namespace)
                                .build())
                .withNewRoleRef().withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .withKind(HasMetadata.getKind(ClusterRole.class))
                .withName(ServiceAccountName)
                .endRoleRef().build();

        return crb;
    }

    private Deployment deployment(String namespace) {
        Probe livenessProbe = new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder().withPath("/").withPort(new IntOrString(7070)).build())
                .withInitialDelaySeconds(30).withPeriodSeconds(3).build();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", Quantity.parse("512Mi"));
        requests.put("cpu", Quantity.parse("500m"));

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("memory", Quantity.parse("1024Mi"));
        limits.put("cpu", Quantity.parse("1000m"));

        Container container = new ContainerBuilder()
                .withName("operator")
                .withImage(ContainerImage)
                .withImagePullPolicy("Always")
                .withPorts(
                        new ContainerPortBuilder().withName("http").withContainerPort(7070).withProtocol("TCP").build())
                .withLivenessProbe(livenessProbe)
                .withNewResources()
                .withRequests(requests).withLimits(limits)
                .endResources()
                .withEnv(new EnvVarBuilder().withName("NAMESPACE").withValue(namespace).build())
                .build();

        Deployment deployment = new DeploymentBuilder()
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
                .withContainers(container)
                .withServiceAccount(ServiceAccountName)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        return deployment;
    }

    private Service service(String namespace) {
        ServicePort httpPort = new ServicePortBuilder()
                .withName("http")
                .withPort(80)
                .withProtocol("TCP")
                .withTargetPort(new IntOrString(7070))
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(ServiceAccountName)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .withNewSpec()
                .withPorts(httpPort)
                .withType("ClusterIP")
                .withSelector(LABELS)
                .endSpec()
                .build();

        return service;
    }
}
