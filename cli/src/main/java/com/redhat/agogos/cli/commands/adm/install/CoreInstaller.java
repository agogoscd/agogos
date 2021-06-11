package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.ClusterStage;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.StepBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceDeclarationBuilder;
import io.fabric8.tekton.triggers.v1alpha1.ClusterTriggerBinding;
import io.fabric8.tekton.triggers.v1alpha1.EventListener;
import io.fabric8.tekton.triggers.v1alpha1.Trigger;
import io.fabric8.tekton.triggers.v1alpha1.TriggerBinding;
import io.fabric8.tekton.triggers.v1alpha1.TriggerTemplate;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
    public static final String CLUSTER_ROLE_NAME_EVENTING = "agogos-el";

    private static final String INIT_TEKTON_TASK_NAME = "init";
    private static final String INIT_TEKTON_TASK_DEFAULT_IMAGE = "quay.io/cpaas/agogos-poc-stage-init:latest";
    private static final String INIT_STAGE_NAME = "init";

    private static final String SA = "agogos";

    private static final Map<String, String> LABELS = Map.of(//
            "app.kubernetes.io/instance", "default", //
            "app.kubernetes.io/part-of", "agogos", //
            "app.kubernetes.io/component", "core"//
    );

    @Inject
    AgogosClient agogosClient;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos core resources...");

        List<HasMetadata> resources = installKubernetesResources( //
                List.of( //
                        namespace(namespace),
                        serviceAccount(),

                        clusterRoleEventing(),
                        initClusterTask()),
                namespace);

        resources.add(installInitClusterStage());
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

    private ServiceAccount serviceAccount() {
        ServiceAccount sa = new ServiceAccountBuilder().withNewMetadata().withName(SA)
                .withLabels(LABELS).endMetadata()
                .build();

        return sa;
    }

    private ClusterRole clusterRoleEventing() {
        return new ClusterRoleBuilder().withNewMetadata().withName(CLUSTER_ROLE_NAME_EVENTING).withLabels(LABELS)
                .endMetadata().withRules( //
                        // Tekton Triggers
                        new PolicyRuleBuilder()
                                .withApiGroups(HasMetadata.getGroup(Trigger.class))
                                .withResources( //
                                        HasMetadata.getPlural(EventListener.class),
                                        HasMetadata.getPlural(TriggerBinding.class),
                                        HasMetadata.getPlural(TriggerTemplate.class),
                                        HasMetadata.getPlural(Trigger.class),
                                        HasMetadata.getPlural(ClusterTriggerBinding.class))
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

    private ClusterTask initClusterTask() {
        ClusterTask ct = new ClusterTaskBuilder() //
                .withNewMetadata() //
                .withName(INIT_TEKTON_TASK_NAME) //
                .endMetadata() //
                .withNewSpec() //
                .withWorkspaces(new WorkspaceDeclarationBuilder().withName("pipeline").build())//
                .withParams( //
                        new ParamSpecBuilder()//
                                .withName("image")//
                                .withType("string")//
                                .withNewDefault()
                                .withStringVal(INIT_TEKTON_TASK_DEFAULT_IMAGE)//
                                .endDefault()//
                                .build(), //
                        new ParamSpecBuilder()//
                                .withName("data")//
                                .withType("string")//
                                .build() //
                ) //
                .withSteps(
                        new StepBuilder()//
                                .withName("execute") //
                                .withImage("$(params.image)")//
                                .withCommand("/bin/python3")//
                                .withArgs("/run.py", "$(params.data)", "$(workspaces.pipeline.path)")//
                                .build()) //
                .endSpec()//
                .build();

        return ct;
    }

    private ClusterStage installInitClusterStage() {
        ClusterStage cs = new ClusterStage();

        cs.getMetadata().setName(INIT_STAGE_NAME);
        // TODO: Change this to be a proper object
        cs.getSpec().getTaskRef().put("kind", "ClusterTask");
        cs.getSpec().getTaskRef().put("name", INIT_TEKTON_TASK_NAME);

        cs = agogosClient.v1alpha1().clusterstages().createOrReplace(cs);

        return cs;
    }
}
