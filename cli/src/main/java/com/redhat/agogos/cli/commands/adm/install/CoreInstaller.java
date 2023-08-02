package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Profile(InstallProfile.dev)
@Profile(InstallProfile.local)
@Profile(InstallProfile.prod)
@Priority(30)
@ApplicationScoped
@RegisterForReflection
public class CoreInstaller extends Installer {
    private static final Logger LOG = LoggerFactory.getLogger(CoreInstaller.class);

    private static final String RESOURCE_NAME_EVENTING = "agogos-eventing";

    public static final String CLUSTER_ROLE_VIEW_NAME = "agogos-view";
    public static final String CLUSTER_ROLE_NAME_EVENTING = "agogos-el";

    public static final Map<String, String> LABELS = Map.of(
            "app.kubernetes.io/part-of", "agogos",
            "app.kubernetes.io/component", "core");

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos core resources...");

        List<HasMetadata> resources = List.of(namespace(namespace), clusterRoleView(), clusterRoleEventing());
        List<HasMetadata> installed = new ArrayList<>();
        for (HasMetadata r : resources) {
            installed.add(kubernetesFacade.serverSideApply(r));
        }

        Helper.status(installed);

        LOG.info("✅ Agogos core resources installed");
    }

    /**
     * <p>
     * Prepares {@link ClusterRole} that is used
     * </p>
     * 
     * @return
     */
    private ClusterRole clusterRoleEventing() {
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

    private ClusterRole clusterRoleView() {
        ClusterRole cr = new ClusterRoleBuilder().withNewMetadata().withName(CLUSTER_ROLE_VIEW_NAME).withLabels(LABELS)
                .withLabels(Map.of("rbac.authorization.k8s.io/aggregate-to-view", "true"))
                .endMetadata().withRules(
                        new PolicyRuleBuilder().withApiGroups("agogos.redhat.com")
                                .withResources("*")
                                .withVerbs("get", "list", "watch")
                                .build())
                .build();

        return cr;
    }

    private Namespace namespace(String namespace) {
        return new NamespaceBuilder().withNewMetadata().withName(namespace).withLabels(LABELS)
                .endMetadata().build();

    }
}
