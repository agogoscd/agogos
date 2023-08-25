package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.triggers.v1alpha1.ClusterInterceptor;
import io.fabric8.tekton.triggers.v1alpha1.Interceptor;
import io.fabric8.tekton.triggers.v1beta1.ClusterTriggerBinding;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.Trigger;
import io.fabric8.tekton.triggers.v1beta1.TriggerBinding;
import io.fabric8.tekton.triggers.v1beta1.TriggerTemplate;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Profile(InstallProfile.prod)
@Priority(30)
@ApplicationScoped
@RegisterForReflection
public class CoreInstaller extends Installer {
    private static final Logger LOG = LoggerFactory.getLogger(CoreInstaller.class);

    private static final String RESOURCE_NAME_EVENTING = "agogos-eventing";

    private static final String AGOGOS_PREFIX = "agogos-";
    public static final String CLUSTER_ROLE_AGOGOS_SA_ADMIN = "agogos-sa-admin";
    public static final String CLUSTER_ROLE_AGOGOS_SA = "agogos-sa";
    public static final String CLUSTER_ROLE_NAME_EVENTING = "agogos-el";

    public static final Map<String, String> LABELS = Map.of(
            "app.kubernetes.io/part-of", "agogos",
            "app.kubernetes.io/component", "core");

    @ConfigProperty(name = "agogos.agogos.service-account")
    private String ServiceAccountName;

    public enum AgogosRole {
        ADMIN("admin", "create", "delete", "get", "list", "patch", "watch"),
        EDIT("edit", "create", "delete", "get", "list", "patch", "watch"),
        VIEW("view", "get", "list", "watch");

        public String name; // The aggregated role, to contain "role"  as well.
        List<String> verbs;
        String role; // The role to be aggregated.

        private AgogosRole(String name, String... verbs) {
            this.name = name;
            this.verbs = Arrays.asList(verbs);
            this.role = AGOGOS_PREFIX + name;
        }
    }

    @Override
    public void install(InstallProfile profile, String createNamespace) {
        LOG.info("🕞 Installing Agogos core resources...");

        List<HasMetadata> resources = new ArrayList<>();
        resources.add(namespace(createNamespace));
        resources.add(createClusterRoleSaNamespace());
        resources.add(createClusterRoleEventing());
        resources.addAll(Stream.of(AgogosRole.values()).map(r -> createClusterRole(r)).collect(Collectors.toList()));
        ServiceAccount sa = createServiceAccount(createNamespace);
        resources.add(sa);
        resources.add(createSaClusterRoleBinding(sa, CLUSTER_ROLE_AGOGOS_SA, CLUSTER_ROLE_AGOGOS_SA));
        resources.add(createSaClusterRoleBinding(sa, CLUSTER_ROLE_AGOGOS_SA_ADMIN, AgogosRole.ADMIN.name));
        resources.add(createSaSecret(sa));

        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata r : resources) {
            installed.add(kubernetesFacade.serverSideApply(r));
        }

        Helper.status(installed);

        LOG.info("✅ Agogos core resources installed");
    }

    private ServiceAccount createServiceAccount(String namespace) {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(ServiceAccountName)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .build();

        return sa;
    }

    private Secret createSaSecret(ServiceAccount sa) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(sa.getMetadata().getName())
                .withNamespace(sa.getMetadata().getNamespace())
                .withAnnotations(Map.of("kubernetes.io/service-account.name", sa.getMetadata().getName()))
                .endMetadata()
                .withType("kubernetes.io/service-account-token")
                .build();
    }

    /**
     * <p>
     * Prepares {@link ClusterRole} that is used
     * </p>
     * 
     * @return
     */
    private ClusterRole createClusterRoleEventing() {
        return new ClusterRoleBuilder().withNewMetadata().withName(RESOURCE_NAME_EVENTING).withLabels(LABELS)
                .endMetadata().withRules(
                        // Tekton Triggers
                        new PolicyRuleBuilder()
                                .withApiGroups(HasMetadata.getGroup(Trigger.class))
                                .withResources(
                                        HasMetadata.getPlural(EventListener.class),
                                        HasMetadata.getPlural(Interceptor.class),
                                        HasMetadata.getPlural(TriggerBinding.class),
                                        HasMetadata.getPlural(TriggerTemplate.class),
                                        HasMetadata.getPlural(Trigger.class),
                                        HasMetadata.getPlural(ClusterTriggerBinding.class),
                                        HasMetadata.getPlural(ClusterInterceptor.class))
                                .withVerbs("get", "list", "watch").build(),
                        new PolicyRuleBuilder().withApiGroups("").withResources(HasMetadata.getPlural(ConfigMap.class))
                                .withVerbs("get", "list", "watch").build(),
                        new PolicyRuleBuilder().withApiGroups("")
                                .withResources(HasMetadata.getPlural(ServiceAccount.class)).withVerbs("impersonate")
                                .build(),
                        new PolicyRuleBuilder().withApiGroups(HasMetadata.getGroup(PipelineRun.class))
                                .withResources(
                                        HasMetadata.getPlural(CustomRun.class),
                                        HasMetadata.getPlural(PipelineRun.class))
                                .withVerbs("create").build())
                .build();
    }

    private ClusterRoleBinding createSaClusterRoleBinding(ServiceAccount sa, String binding, String role) {
        return new ClusterRoleBindingBuilder()
                .withNewMetadata()
                .withName(binding)
                .withNamespace(sa.getMetadata().getNamespace())
                .endMetadata()
                .addNewSubject()
                .withApiGroup(HasMetadata.getGroup(sa.getClass()))
                .withKind(sa.getKind())
                .withName(sa.getMetadata().getName())
                .withNamespace(sa.getMetadata().getNamespace())
                .endSubject()
                .withNewRoleRef()
                .withKind(HasMetadata.getKind(ClusterRole.class))
                .withName(role)
                .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .endRoleRef()
                .build();
    }

    private ClusterRole createClusterRole(AgogosRole role) {
        ClusterRole cr = new ClusterRoleBuilder().withNewMetadata().withName(role.role).withLabels(LABELS)
                .withLabels(Map.of("rbac.authorization.k8s.io/aggregate-to-" + role.name, "true"))
                .endMetadata().withRules(
                        new PolicyRuleBuilder().withApiGroups("agogos.redhat.com")
                                .withResources("*")
                                .withVerbs(role.verbs)
                                .build())
                .build();

        return cr;
    }

    private ClusterRole createClusterRoleSaNamespace() {
        return new ClusterRoleBuilder().withNewMetadata().withName(CLUSTER_ROLE_AGOGOS_SA).withLabels(LABELS)
                .endMetadata().withRules(
                        new PolicyRuleBuilder()
                                .withApiGroups("")
                                .withResources(
                                        HasMetadata.getPlural(Namespace.class),
                                        HasMetadata.getPlural(ResourceQuota.class))
                                .withVerbs("create", "delete", "get", "list", "patch", "watch").build(),
                        new PolicyRuleBuilder()
                                .withApiGroups(HasMetadata.getGroup(ClusterRole.class))
                                .withResources(
                                        HasMetadata.getPlural(ClusterRole.class),
                                        HasMetadata.getPlural(ClusterRoleBinding.class))
                                .withVerbs("create", "delete", "get", "list", "patch", "watch").build())
                .build();
    }

    private Namespace namespace(String namespace) {
        return new NamespaceBuilder().withNewMetadata().withName(namespace).withLabels(LABELS)
                .endMetadata().build();

    }
}
