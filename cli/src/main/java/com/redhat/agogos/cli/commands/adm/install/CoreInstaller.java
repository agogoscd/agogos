package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerBuilder;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.triggers.v1alpha1.ClusterTriggerBinding;
import io.fabric8.tekton.triggers.v1alpha1.EventListener;
import io.fabric8.tekton.triggers.v1alpha1.TriggerBinding;
import io.fabric8.tekton.triggers.v1alpha1.TriggerTemplate;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(30)
@ApplicationScoped
@RegisterForReflection
public class CoreInstaller extends Installer {

    private static final Logger LOG = LoggerFactory.getLogger(CoreInstaller.class);
    private static final String KNATIVE_BROKER_NAME = "default";
    private static final String ROLE_NAME_EVENTING = "agogos-el";

    private static final Map<String, String> LABELS = Map.of(//
            "app.kubernetes.io/instance", "default", //
            "app.kubernetes.io/part-of", "agogos", //
            "app.kubernetes.io/component", "core"//
    );

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos core resources...");

        List<HasMetadata> resources = installKubernetesResources( //
                List.of( //
                        namespace(namespace),
                        clusterRoleEventing()
                //clusterRoleBinding(namespace) //
                ),
                namespace);

        resources.add(installKnativeBroker(namespace));

        status(resources);

        LOG.info("✅ Agogos core resources installed");
    }

    private Broker installKnativeBroker(String namespace) {
        Broker broker = new BrokerBuilder().withNewMetadata().withName(KNATIVE_BROKER_NAME)
                .withLabels(LABELS).endMetadata().build();

        broker = kubernetesClient.resource(broker).inNamespace(namespace).deletingExisting().createOrReplace();

        return broker;
    }

    private ClusterRole clusterRoleEventing() {
        return new ClusterRoleBuilder().withNewMetadata().withName(ROLE_NAME_EVENTING).withLabels(LABELS)
                .endMetadata().withRules( //
                        new PolicyRuleBuilder().withApiGroups(HasMetadata.getGroup(Trigger.class))
                                .withResources(HasMetadata.getPlural(ClusterTriggerBinding.class))
                                .withVerbs("get", "list", "watch").build(), //
                        new PolicyRuleBuilder().withApiGroups(HasMetadata.getGroup(Trigger.class))
                                .withResources(HasMetadata.getPlural(EventListener.class),
                                        HasMetadata.getPlural(TriggerBinding.class),
                                        HasMetadata.getPlural(TriggerTemplate.class),
                                        HasMetadata.getPlural(Trigger.class))
                                .withVerbs("get", "list", "watch").build(), //
                        new PolicyRuleBuilder().withApiGroups("").withResources(HasMetadata.getPlural(ConfigMap.class))
                                .withVerbs("get", "list", "watch").build(), //
                        new PolicyRuleBuilder().withApiGroups("")
                                .withResources(HasMetadata.getPlural(ServiceAccount.class)).withVerbs("impersonate")
                                .build(), //
                        new PolicyRuleBuilder().withApiGroups(HasMetadata.getGroup(PipelineRun.class))
                                .withResources(HasMetadata.getPlural(PipelineRun.class)).withVerbs("create").build() //
                ).build();

    }

    private Namespace namespace(String namespace) {
        return new NamespaceBuilder().withNewMetadata().withName(namespace).withLabels(LABELS)
                .endMetadata().build();

    }
}
