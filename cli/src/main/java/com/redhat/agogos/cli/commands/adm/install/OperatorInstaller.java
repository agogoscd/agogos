package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profiles({ @Profile(InstallProfile.local) })
@Priority(99)
@ApplicationScoped
@RegisterForReflection
public class OperatorInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(OperatorInstaller.class);
    private static final String CONTAINER_IMAGE = "quay.io/cpaas/agogos-poc-operator:devel";

    private static final String OPERATOR = "agogos-operator";

    private static final Map<String, String> LABELS = Map.of(//
            "app.kubernetes.io/instance", "default", //
            "app.kubernetes.io/part-of", "agogos", //
            "app.kubernetes.io/component", "operator"//
    );

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos Operator component...");

        List<HasMetadata> resources = installKubernetesResources( //
                List.of( //
                        serviceAccount(),
                        clusterRole(),
                        clusterRoleBinding(namespace), //
                        service(), //
                        deployment() //
                ),
                namespace);

        status(resources);

        LOG.info("✅ Agogos Operator installed");
    }

    private ServiceAccount serviceAccount() {
        ServiceAccount sa = new ServiceAccountBuilder().withNewMetadata().withName(OPERATOR)
                .withLabels(LABELS).endMetadata()
                .build();

        return sa;
    }

    private ClusterRole clusterRole() {
        ClusterRole cr = new ClusterRoleBuilder().withNewMetadata().withName(OPERATOR).withLabels(LABELS)
                .endMetadata().withRules( //
                        new PolicyRuleBuilder().withApiGroups("agogos.redhat.com")
                                .withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build(), //
                        new PolicyRuleBuilder().withApiGroups("apiextensions.k8s.io").withResources("customresourcedefinitions")
                                .withVerbs("get", "list", "watch").build(), //
                        new PolicyRuleBuilder().withApiGroups("tekton.dev").withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build(), //
                        new PolicyRuleBuilder().withApiGroups("triggers.tekton.dev").withResources("*")
                                .withVerbs("get", "list", "watch", "create", "update", "patch", "delete", "deletecollection")
                                .build() //
                )
                .build();

        return cr;
    }

    private ClusterRoleBinding clusterRoleBinding(String namespace) {

        ClusterRoleBinding crb = new ClusterRoleBindingBuilder().withNewMetadata().withName(OPERATOR)
                .withLabels(LABELS).endMetadata()
                .withSubjects(
                        new SubjectBuilder().withKind(HasMetadata.getKind(ServiceAccount.class)).withName(OPERATOR)
                                .withNamespace(namespace)
                                .build())
                .withNewRoleRef().withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .withKind(HasMetadata.getKind(ClusterRole.class))
                .withName(OPERATOR)
                .endRoleRef().build();

        return crb;
    }

    private Deployment deployment() {
        Probe livenessProbe = new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder().withPath("/").withPort(new IntOrString(7070)).build())
                .withInitialDelaySeconds(3).withPeriodSeconds(3).build();

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", Quantity.parse("512Mi"));
        requests.put("cpu", Quantity.parse("500m"));

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("memory", Quantity.parse("1024Mi"));
        limits.put("cpu", Quantity.parse("1000m"));

        Container container = new ContainerBuilder()
                .withName("operator").withImage(CONTAINER_IMAGE).withImagePullPolicy("Always") //
                .withPorts(//
                        new ContainerPortBuilder().withName("http").withContainerPort(7070).withProtocol("TCP").build() //
                )//
                .withLivenessProbe(livenessProbe) //
                .withNewResources() //
                .withRequests(requests).withLimits(limits) //
                .endResources() //
                .build();

        Deployment deployment = new DeploymentBuilder().withNewMetadata().withName(OPERATOR)
                .withLabels(LABELS)
                .endMetadata().withNewSpec().withReplicas(1).withNewSelector().withMatchLabels(LABELS).endSelector()
                .withNewTemplate().withNewMetadata().withLabels(LABELS).endMetadata().withNewSpec()
                .withContainers(container)
                .withServiceAccount(OPERATOR)
                .endSpec().endTemplate().endSpec().build();

        return deployment;
    }

    private Service service() {
        ServicePort httpPort = new ServicePortBuilder().withName("http").withPort(80).withProtocol("TCP")
                .withTargetPort(new IntOrString(7070)).build();

        Service service = new ServiceBuilder().withNewMetadata().withName(OPERATOR).withLabels(LABELS)
                .endMetadata().withNewSpec().withPorts(httpPort).withType("ClusterIP").withSelector(LABELS)
                .endSpec()
                .build();

        return service;
    }
}
