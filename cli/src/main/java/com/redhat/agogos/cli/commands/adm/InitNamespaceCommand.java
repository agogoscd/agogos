package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.AbstractCallableSubcommand;
import com.redhat.agogos.cli.commands.adm.install.CoreInstaller;
import com.redhat.agogos.cli.commands.adm.install.CoreInstaller.AgogosRole;
import com.redhat.agogos.core.v1alpha1.Dependency;
import com.redhat.agogos.core.v1alpha1.Submission;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerBuilder;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.fabric8.tekton.pipeline.v1beta1.CustomRunBuilder;
import io.fabric8.tekton.triggers.v1alpha1.ClusterInterceptor;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.EventListenerBuilder;
import io.fabric8.tekton.triggers.v1beta1.InterceptorParams;
import io.fabric8.tekton.triggers.v1beta1.InterceptorParamsBuilder;
import io.fabric8.tekton.triggers.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptor;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptorBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerSpecBinding;
import io.fabric8.tekton.triggers.v1beta1.TriggerSpecBindingBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerSpecTemplateBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerTemplateSpec;
import io.fabric8.tekton.triggers.v1beta1.TriggerTemplateSpecBuilder;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Command(mixinStandardHelpOptions = true, name = "init-namespace", aliases = {
        "init" }, description = "Initialize selected namespace to work with Agogos")
public class InitNamespaceCommand extends AbstractCallableSubcommand {

    @Inject
    KubernetesSerialization objectMapper;

    private static final Logger LOG = LoggerFactory.getLogger(InitNamespaceCommand.class);

    private static final String AGOGOS_QUOTA_NAME = "agogos-quota";
    private static final String DEPENDENCY_CEL_INTERCEPTOR_FILTER = String.join(" || ", List.of(
            "header.match('ce-type', 'com.redhat.agogos.event.build.succeeded.v1alpha1')",
            "header.match('ce-type', 'com.redhat.agogos.event.execution.succeeded.v1alpha1')",
            "header.match('ce-type', 'com.redhat.agogos.event.run.succeeded.v1alpha1')"));
    private static final String DEPENDENCY_CEL_INTERCEPTOR_NAME_OVERLAY = "has(body.build) ? body.build.metadata.labels['agogos.redhat.com/name'] "
            +
            ": has(body.execution) ? body.execution.metadata.labels['agogos.redhat.com/name'] " +
            ": has(body.run) ? body.run.metadata.labels['agogos.redhat.com/name'] : ''";
    private static final String DEPENDENCY_CEL_INTERCEPTOR_INSTANCE_OVERLAY = "has(body.build) ? body.build.metadata.labels['agogos.redhat.com/instance'] "
            +
            ": has(body.execution) ? body.execution.metadata.labels['agogos.redhat.com/instance'] " +
            ": has(body.run) ? body.run.metadata.labels['agogos.redhat.com/instance'] : ''";
    private static final String DEPENDENCY_CEL_INTERCEPTOR_RESOURCE_OVERLAY = "has(body.build) ? body.build.metadata.labels['agogos.redhat.com/resource'] "
            +
            ": has(body.execution) ? body.execution.metadata.labels['agogos.redhat.com/resource'] " +
            ": has(body.run) ? body.run.metadata.labels['agogos.redhat.com/resource'] : ''";
    private static final String SUBMISSION_CEL_INTERCEPTOR_FILTER = String.join(" || ", List.of(
            "header.match('ce-type', 'com.redhat.agogos.event.component.build.v1alpha1')",
            "header.match('ce-type', 'com.redhat.agogos.event.group.execution.v1alpha1')",
            "header.match('ce-type', 'com.redhat.agogos.event.pipeline.run.v1alpha1')"));
    private static final String SUBMISSION_CEL_INTERCEPTOR_GROUP_OVERLAY = "has(body.group) ? body.group : ''";
    private static final String RESOURCE_NAME = "agogos";
    private static final String RESOURCE_NAME_CONFIG = "agogos-config";
    private static final String RESOURCE_NAME_EVENTING = "agogos-eventing";

    private static final Map<String, String> LABELS = Map.of(
            "app.kubernetes.io/part-of", "agogos",
            "app.kubernetes.io/component", "namespace");

    @ConfigProperty(name = "agogos.operator.service-account")
    private String ServiceAccountName;

    @Option(names = { "--namespace", "-n" }, required = true, description = "Name of the namespace to be initialized")
    String namespace;

    @Option(names = { "--admins" }, split = ",", description = "List of users given the admin role for the namespace")
    Set<String> admin;

    @Option(names = { "--editors" }, split = ",", description = "List of users given the edit role for the namespace")
    Set<String> editor;

    @Option(names = { "--viewers" }, split = ",", description = "List of users given the view role for the namespace")
    Set<String> viewer;

    @Option(names = { "--quota-file" }, description = "Resource quota file to be applied to the namespace")
    File quotaFile;

    @Override
    public Integer call() {
        LOG.info("🕞 Initializing '{}' namespace with Agogos resources...", namespace);

        if (isAgogosCoreNamespace()) {
            LOG.error("⛔ Unable to initialize namespace '{}' as it is an Agogos core namespace.", namespace);
            return CommandLine.ExitCode.USAGE;
        }

        installNamespace();
        installConfig();

        ServiceAccount sa = installMainSa();
        installMainRoleBinding(sa);

        ServiceAccount eventingSa = installEventingSa(namespace, LABELS);
        installEventingRoleBinding(eventingSa, namespace);
        ConfigMap configMap = installBrokerConfig(namespace);
        EventListener el = installTektonEl(eventingSa, namespace);
        Broker broker = installKnativeBroker(configMap, namespace, LABELS);
        installKnativeTrigger(broker, el, namespace);
        installSubmissionTrigger(namespace);
        installDependencyTrigger(namespace);

        List<Map.Entry<AgogosRole, Set<String>>> bindings = Arrays.asList(
                new AbstractMap.SimpleEntry<AgogosRole, Set<String>>(AgogosRole.ADMIN, admin),
                new AbstractMap.SimpleEntry<AgogosRole, Set<String>>(AgogosRole.EDIT, editor),
                new AbstractMap.SimpleEntry<AgogosRole, Set<String>>(AgogosRole.VIEW, viewer));
        installAgogosRoleBindings(bindings);

        installAgogosQuota();

        kubernetesFacade.waitForAllPodsRunning(namespace);

        LOG.info("✅ Namespace '{}' initialized and ready to use!", namespace);
        return CommandLine.ExitCode.OK;
    }

    /**
     * <p>
     * A way to provide custom configuration for {@link com.com.redhat.agogos.core.v1alpha1.Builder} and
     * {@link com.com.redhat.agogos.core.v1alpha1.Stage}.
     * </p>
     *
     * TODO: Rethink this! This should be done differently, maybe.
     */
    private void installConfig() {
        String exampleData = new StringBuilder()
                .append("# This content is not used and is provided as an example.")
                .append(System.getProperty("line.separator"))
                .append("# Please refer to Agogos Stage and Builder documentation.")
                .toString();

        Map<String, String> data = Map.of("_example", exampleData);

        ConfigMap cm = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME_CONFIG)
                .withNamespace(namespace)
                .endMetadata()
                .withData(data)
                .build();

        cm = kubernetesFacade.serverSideApply(cm);

        Helper.status(cm);
    }

    /**
     * <p>
     * Prepares the namespace for the new project.
     * </p>
     */
    private void installNamespace() {
        Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .build();

        ns = kubernetesFacade.serverSideApply(ns);

        Helper.status(ns);
    }

    private void installMainRoleBinding(ServiceAccount sa) {
        RoleBinding roleBinding = new RoleBindingBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .endMetadata()
                .addNewSubject()
                .withApiGroup(HasMetadata.getGroup(sa.getClass()))
                .withKind(sa.getKind())
                .withName(sa.getMetadata().getName())
                .endSubject()
                .withNewRoleRef()
                .withKind(HasMetadata.getKind(ClusterRole.class))
                .withName(AgogosRole.VIEW.name)
                .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .endRoleRef()
                .build();

        roleBinding = kubernetesFacade.serverSideApply(roleBinding);

        Helper.status(roleBinding);
    }

    /**
     * Installs ServiceAccount used by Pipelines.
     */
    private ServiceAccount installMainSa() {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .withLabels(LABELS)
                .endMetadata()
                .build();

        sa = kubernetesFacade.serverSideApply(sa);

        Helper.status(sa);

        return sa;
    }

    private ConfigMap installBrokerConfig(String namespace) {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName("agogos-broker-config")
                .withNamespace(namespace)
                .endMetadata()
                .withData(Map.of("channelTemplateSpec", "apiVersion: messaging.knative.dev/v1\nkind: InMemoryChannel"))
                .build();

        configMap = kubernetesFacade.serverSideApply(configMap);

        Helper.status(configMap);

        return configMap;
    }

    private Broker installKnativeBroker(ConfigMap configuration, String namespace, Map<String, String> labels) {
        Broker broker = new BrokerBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .withLabels(labels)
                .withAnnotations(Map.of("eventing.knative.dev/broker.class", "MTChannelBasedBroker")) // TODO: Not good for production deployment, fine for now
                .endMetadata()
                .withNewSpec()
                .withNewConfig()
                .withApiVersion(configuration.getApiVersion())
                .withKind(configuration.getKind())
                .withName(configuration.getMetadata().getName())
                .withNamespace(configuration.getMetadata().getNamespace())
                .endConfig()
                .endSpec()
                .build();

        broker = kubernetesFacade.serverSideApply(broker);

        Helper.status(broker);

        return broker;
    }

    /**
     * <p>
     * Prepares Tekton {@link EventListener} responsible for handling CloudEvents coming from the broker.
     * </p>
     */
    private EventListener installTektonEl(ServiceAccount sa, String namespace) {
        EventListener el = new EventListenerBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(sa.getMetadata().getName())
                .withNewNamespaceSelector()
                .withMatchNames(namespace)
                .endNamespaceSelector()
                .endSpec()
                .build();

        el = kubernetesFacade.serverSideApply(el);
        el = kubernetesFacade.waitForEventListenerRunning(namespace, el.getMetadata().getName());

        Helper.status(el);

        return el;
    }

    /**
     * Install Knative Trigger responsible for routing events from the Broker into Tekton EventListener.
     */
    private Trigger installKnativeTrigger(Broker broker, EventListener el, String namespace) {
        Trigger trigger = new TriggerBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBroker(broker.getMetadata().getName())
                .withNewSubscriber()
                .withUri(el.getStatus().getAddress().getUrl())
                .endSubscriber()
                .endSpec()
                .build();

        trigger = kubernetesFacade.serverSideApply(trigger);

        Helper.status(trigger);

        return trigger;
    }

    /**
     * Ensure the {@link ClusterRoleBinding} for the {@link ServiceAccount} used by the Tekton {@link EventListener} exists and
     * is configured properly.
     *
     */
    private ClusterRoleBinding installEventingRoleBinding(ServiceAccount sa, String namespace) {
        ClusterRoleBinding roleBinding = kubernetesFacade.get(ClusterRoleBinding.class, RESOURCE_NAME_EVENTING);
        if (roleBinding == null) {
            roleBinding = new ClusterRoleBindingBuilder()
                    .withNewMetadata()
                    .withName(RESOURCE_NAME_EVENTING)
                    .endMetadata()
                    .withNewRoleRef()
                    .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                    .withKind(HasMetadata.getKind(ClusterRole.class))
                    .withName(RESOURCE_NAME_EVENTING)
                    .endRoleRef()
                    .build();
        }

        Subject subject = new SubjectBuilder()
                .withKind(sa.getKind())
                .withName(sa.getMetadata().getName())
                .withNamespace(namespace)
                .build();

        if (!roleBinding.getSubjects().contains(subject)) {
            roleBinding.getSubjects().add(subject);
            roleBinding = kubernetesFacade.serverSideApply(roleBinding);
        }
        Helper.status(roleBinding);

        return roleBinding;
    }

    private ServiceAccount installEventingSa(String namespace, Map<String, String> labels) {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME_EVENTING)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .build();

        sa = kubernetesFacade.serverSideApply(sa);

        Helper.status(sa);

        return sa;
    }

    private void installAgogosRoleBindings(List<Map.Entry<AgogosRole, Set<String>>> bindings) {
        Set<String> processed = new HashSet<>(); // Used to ensure each user ends up with only one role.

        for (Map.Entry<AgogosRole, Set<String>> e : bindings) {

            if (e.getValue() == null) {
                // Remove rolebinding if no users specified.
                kubernetesFacade.delete(RoleBinding.class, namespace, e.getKey().name);
                continue;
            }

            List<Subject> subjects = e.getValue().stream()
                    .filter(id -> !processed.contains(id))
                    .map(id -> new SubjectBuilder()
                            .withName(id)
                            .withNamespace(namespace)
                            .withKind("User")
                            .withApiGroup(HasMetadata.getGroup(Subject.class))
                            .build())
                    .collect(Collectors.toList());

            if (subjects.size() == 0) {
                // Remove rolebinding if no users would be in it.
                kubernetesFacade.delete(RoleBinding.class, namespace, e.getKey().name);
                continue;
            }

            RoleBinding roleBinding = new RoleBindingBuilder()
                    .withNewMetadata()
                    .withName(e.getKey().name)
                    .withNamespace(namespace)
                    .withLabels(LABELS)
                    .endMetadata()
                    .withSubjects(subjects)
                    .withNewRoleRef()
                    .withKind(HasMetadata.getKind(ClusterRole.class))
                    .withName(e.getKey().name)
                    .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                    .endRoleRef()
                    .build();

            roleBinding = kubernetesFacade.serverSideApply(roleBinding);
            Helper.status(roleBinding);

            processed.addAll(e.getValue()); // Add all new users as processed.
        }
    }

    private void installAgogosQuota() {
        if (quotaFile == null) {
            if (kubernetesFacade.get(ResourceQuota.class, namespace, AGOGOS_QUOTA_NAME) != null) {
                kubernetesFacade.delete(ResourceQuota.class, namespace, AGOGOS_QUOTA_NAME);
            }
            return;
        }

        try {
            ResourceQuota resourceQuota = kubernetesFacade.unmarshal(ResourceQuota.class, new FileInputStream(quotaFile));

            resourceQuota = new ResourceQuotaBuilder(resourceQuota)
                    .withNewMetadata()
                    .withName(AGOGOS_QUOTA_NAME)
                    .withNamespace(namespace)
                    .endMetadata()
                    .build();
            resourceQuota = kubernetesFacade.serverSideApply(resourceQuota);

            Helper.status(resourceQuota);
        } catch (FileNotFoundException e) {
            LOG.error("File " + quotaFile.getName() + " not found, no resource quota applied", e);
        }
    }

    private boolean isAgogosCoreNamespace() {
        Namespace ns = kubernetesFacade.get(Namespace.class, namespace);
        if (ns != null) {
            for (String label : CoreInstaller.LABELS.keySet()) {
                if (!CoreInstaller.LABELS.get(label).equals(ns.getMetadata().getLabels().get(label))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void installSubmissionTrigger(String namespace) {
        InterceptorParams filter = new InterceptorParamsBuilder()
                .withName("filter")
                .withValue(SUBMISSION_CEL_INTERCEPTOR_FILTER)
                .build();
        InterceptorParams overlays = new InterceptorParamsBuilder()
                .withName("overlays")
                .withValue(List.of(
                        Map.of("key", "group", "expression", SUBMISSION_CEL_INTERCEPTOR_GROUP_OVERLAY)))
                .build();

        TriggerInterceptor interceptor = new TriggerInterceptorBuilder()
                .withNewRef()
                .withName("cel")
                .withKind(HasMetadata.getKind(ClusterInterceptor.class))
                .endRef()
                .withParams(filter, overlays)
                .build();

        TriggerSpecBinding nameBinding = new TriggerSpecBindingBuilder()
                .withName("name")
                .withValue("$(body.name)")
                .build();
        TriggerSpecBinding resourceBinding = new TriggerSpecBindingBuilder()
                .withName("resource")
                .withValue("$(body.resource)")
                .build();
        TriggerSpecBinding instanceBinding = new TriggerSpecBindingBuilder()
                .withName("instance")
                .withValue("$(body.instance)")
                .build();
        TriggerSpecBinding groupBinding = new TriggerSpecBindingBuilder()
                .withName("group")
                .withValue("$(extensions.group)")
                .build();

        CustomRun run = new CustomRunBuilder()
                .withNewMetadata()
                .withGenerateName("agogos-submission-trigger-custom-run-")
                .endMetadata()
                .withNewSpec()
                .withNewCustomSpec()
                .withApiVersion(HasMetadata.getApiVersion(Submission.class))
                .withKind(HasMetadata.getKind(Submission.class))
                .addToSpec("name", "$(tt.params.name)")
                .addToSpec("resource", "$(tt.params.resource)")
                .addToSpec("instance", "$(tt.params.instance)")
                .addToSpec("group", "$(tt.params.group)")
                .endCustomSpec()
                .endSpec()
                .build();
        TriggerTemplateSpec template = new TriggerTemplateSpecBuilder()
                .addToParams(new ParamSpecBuilder().withName("name").withDefault("$(tt.params.name)").build())
                .addToParams(new ParamSpecBuilder().withName("resource").withDefault("$(tt.params.resource)").build())
                .addToParams(new ParamSpecBuilder().withName("instance").withDefault("$(tt.params.instance)").build())
                .addToParams(new ParamSpecBuilder().withName("group").withDefault("$(tt.params.group)").build())
                .withResourcetemplates(run)
                .build();
        io.fabric8.tekton.triggers.v1beta1.TriggerBuilder builder = new io.fabric8.tekton.triggers.v1beta1.TriggerBuilder();
        builder.withNewMetadata()
                .withName("agogos-submission")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBindings(nameBinding, resourceBinding, instanceBinding, groupBinding)
                .withInterceptors(interceptor)
                .withTemplate(new TriggerSpecTemplateBuilder().withSpec(template).build())
                .withNewTemplate()
                .withSpec(template)
                .endTemplate()
                .endSpec();

        io.fabric8.tekton.triggers.v1beta1.Trigger trigger = kubernetesFacade.serverSideApply(builder.build());
        Helper.status(trigger);
    }

    private void installDependencyTrigger(String namespace) {
        InterceptorParams filter = new InterceptorParamsBuilder()
                .withName("filter")
                .withValue(DEPENDENCY_CEL_INTERCEPTOR_FILTER)
                .build();
        InterceptorParams overlays = new InterceptorParamsBuilder()
                .withName("overlays")
                .withValue(List.of(
                        Map.of("key", "name", "expression", DEPENDENCY_CEL_INTERCEPTOR_NAME_OVERLAY),
                        Map.of("key", "instance", "expression", DEPENDENCY_CEL_INTERCEPTOR_INSTANCE_OVERLAY),
                        Map.of("key", "resource", "expression", DEPENDENCY_CEL_INTERCEPTOR_RESOURCE_OVERLAY)))
                .build();

        TriggerInterceptor celInterceptor = new TriggerInterceptorBuilder()
                .withNewRef()
                .withName("cel")
                .withKind(HasMetadata.getKind(ClusterInterceptor.class))
                .endRef()
                .withParams(filter, overlays)
                .build();

        TriggerSpecBinding nameBinding = new TriggerSpecBindingBuilder()
                .withName("name")
                .withValue("$(extensions.name)")
                .build();

        TriggerSpecBinding instanceBinding = new TriggerSpecBindingBuilder()
                .withName("instance")
                .withValue("$(extensions.instance)")
                .build();

        TriggerSpecBinding resourceBinding = new TriggerSpecBindingBuilder()
                .withName("resource")
                .withValue("$(extensions.resource)")
                .build();

        CustomRun run = new CustomRunBuilder()
                .withNewMetadata()
                .withGenerateName("agogos-dependency-trigger-custom-run-")
                .endMetadata()
                .withNewSpec()
                .withNewCustomSpec()
                .withApiVersion(HasMetadata.getApiVersion(Dependency.class))
                .withKind(HasMetadata.getKind(Dependency.class))
                .addToSpec("name", "$(tt.params.name)")
                .addToSpec("instance", "$(tt.params.instance)")
                .addToSpec("resource", "$(tt.params.resource)")
                .endCustomSpec()
                .endSpec()
                .build();

        TriggerTemplateSpec template = new TriggerTemplateSpecBuilder()
                .addToParams(new ParamSpecBuilder().withName("name").withDefault("$(tt.params.name)").build())
                .addToParams(new ParamSpecBuilder().withName("instance").withDefault("$(tt.params.instance)").build())
                .addToParams(new ParamSpecBuilder().withName("resource").withDefault("$(tt.params.resource)").build())
                .withResourcetemplates(run)
                .build();

        io.fabric8.tekton.triggers.v1beta1.TriggerBuilder builder = new io.fabric8.tekton.triggers.v1beta1.TriggerBuilder();
        builder.withNewMetadata()
                .withName("agogos-dependency")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBindings(nameBinding, instanceBinding, resourceBinding)
                .withInterceptors(celInterceptor)
                .withTemplate(new TriggerSpecTemplateBuilder().withSpec(template).build())
                .withNewTemplate()
                .withSpec(template)
                .endTemplate()
                .endSpec();

        io.fabric8.tekton.triggers.v1beta1.Trigger trigger = kubernetesFacade.serverSideApply(builder.build());
        Helper.status(trigger);
    }
}
