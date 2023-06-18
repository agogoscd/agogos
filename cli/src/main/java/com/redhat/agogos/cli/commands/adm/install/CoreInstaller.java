package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.ClusterStage;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.StepBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceDeclarationBuilder;
import io.fabric8.tekton.triggers.v1alpha1.ClusterInterceptor;
import io.fabric8.tekton.triggers.v1alpha1.Interceptor;
import io.fabric8.tekton.triggers.v1beta1.ClusterTriggerBinding;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.Trigger;
import io.fabric8.tekton.triggers.v1beta1.TriggerBinding;
import io.fabric8.tekton.triggers.v1beta1.TriggerTemplate;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile(InstallProfile.local)
@Profile(InstallProfile.dev)
@Priority(30)
@ApplicationScoped
@RegisterForReflection
public class CoreInstaller extends Installer {
    private static final Logger LOG = LoggerFactory.getLogger(CoreInstaller.class);

    private static final String RESOURCE_NAME_EVENTING = "agogos-eventing";

    public static final String CLUSTER_ROLE_VIEW_NAME = "agogos-view";
    public static final String CLUSTER_ROLE_NAME_EVENTING = "agogos-el";

    private static final String INIT_TEKTON_TASK_NAME = "init";
    private static final String INIT_STAGE_NAME = "init";

    private Map<String, String> labels = Stream.of(new String[][] {
            { "app.kubernetes.io/part-of", "agogos" },
            { "app.kubernetes.io/component", "core" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    @ConfigProperty(name = "agogos.container-image.init", defaultValue = "quay.io/agogos/stage-init:v1")
    String containerImageInit;

    @Inject
    AgogosClient agogosClient;

    @Inject
    BrokerInstaller brokerInstaller;

    @Inject
    KnativeClient knativeClient;

    @Override
    public void install(InstallProfile profile, String namespace) {
        LOG.info("🕞 Installing Agogos core resources...");

        labels.put("app.kubernetes.io/instance", namespace);

        List<HasMetadata> resources = resourceLoader.installKubernetesResources(
                List.of(
                        namespace(namespace),
                        clusterRoleView(),
                        clusterRoleEventing(),
                        initClusterTask()),
                namespace);

        resources.add(installInitClusterStage());
        resources.addAll(brokerInstaller.install(namespace));

        Helper.status(resources);

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
        return new ClusterRoleBuilder().withNewMetadata().withName(RESOURCE_NAME_EVENTING).withLabels(labels)
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
        ClusterRole cr = new ClusterRoleBuilder().withNewMetadata().withName(CLUSTER_ROLE_VIEW_NAME).withLabels(labels)
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
        return new NamespaceBuilder().withNewMetadata().withName(namespace).withLabels(labels)
                .endMetadata().build();

    }

    private ClusterTask initClusterTask() {
        ClusterTask ct = new ClusterTaskBuilder()
                .withNewMetadata()
                .withName(INIT_TEKTON_TASK_NAME)
                .endMetadata()
                .withNewSpec()
                .withWorkspaces(new WorkspaceDeclarationBuilder().withName("output").build())
                .withParams(
                        new ParamSpecBuilder()
                                .withName("image")
                                .withType("string")
                                .withNewDefault()
                                .withStringVal(containerImageInit)
                                .endDefault()
                                .build(),
                        new ParamSpecBuilder()
                                .withName("resource")
                                .withType("string")
                                .build())
                .withSteps(
                        new StepBuilder()
                                .withName("execute")
                                .withImage("$(params.image)")
                                .withCommand("/usr/local/bin/entrypoint")
                                .withArgs("$(params.resource)", "$(workspaces.output.path)")
                                .build())
                .endSpec()
                .build();

        return ct;
    }

    private ClusterStage installInitClusterStage() {
        ClusterStage cs = new ClusterStage();

        cs.getMetadata().setName(INIT_STAGE_NAME);
        cs.getMetadata().setLabels(labels);
        cs.getSpec().getTaskRef().setKind(HasMetadata.getKind(ClusterTask.class));
        cs.getSpec().getTaskRef().setName(INIT_TEKTON_TASK_NAME);

        cs = agogosClient.v1alpha1().clusterstages().resource(cs).serverSideApply();

        return cs;
    }
}
