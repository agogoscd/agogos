package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.AbstractRunnableSubcommand;
import com.redhat.agogos.cli.commands.adm.install.CoreInstaller;
import com.redhat.agogos.errors.ApplicationException;
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
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.EventListenerBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Command(mixinStandardHelpOptions = true, name = "init-namespace", aliases = {
        "init" }, description = "Initialize selected namespace to work with Agogos")
public class InitNamespaceCommand extends AbstractRunnableSubcommand {

    @ConfigProperty(name = "agogos.cloud-events.base-url", defaultValue = "http://broker-ingress.knative-eventing.svc.cluster.local")
    String baseUrl;

    private static final Logger LOG = LoggerFactory.getLogger(InitNamespaceCommand.class);

    private static final String AGOGOS_QUOTA_NAME = "agogos-quota";
    private static final String AGOGOS_ROLE_BINDING_PREFIX = "agogos-";
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

    private List<HasMetadata> installedResources = new ArrayList<>();

    @Override
    public void run() {
        if (isAgogosCoreNamespace()) {
            LOG.error("⛔ Unable to initialize namespace '{}' as it is an Agogos core namespace.", namespace);
            System.exit(picocli.CommandLine.ExitCode.USAGE);
        }
        LOG.info("🕞 Initializing '{}' namespace with Agogos resources...", namespace);

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

        List<Map.Entry<String, Set<String>>> bindings = Arrays.asList(
                new AbstractMap.SimpleEntry<String, Set<String>>("admin", admin),
                new AbstractMap.SimpleEntry<String, Set<String>>("edit", editor),
                new AbstractMap.SimpleEntry<String, Set<String>>("view", viewer));
        installAgogosRoleBindings(bindings);

        installAgogosQuota();

        Helper.status(installedResources);

        kubernetesFacade.waitForAllPodsRunning(namespace);

        LOG.info("✅ Namespace '{}' initialized and ready to use!", namespace);
    }

    /**
     * <p>
     * A way to provide custom configuration for {@link com.redhat.agogos.v1alpha1.Builder} and
     * {@link com.redhat.agogos.v1alpha1.Stage}.
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

        installedResources.add(cm);
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

        installedResources.add(ns);
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
                .withName(CoreInstaller.CLUSTER_ROLE_VIEW_NAME)
                .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                .endRoleRef()
                .build();

        roleBinding = kubernetesFacade.serverSideApply(roleBinding);

        installedResources.add(roleBinding);
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

        installedResources.add(sa);

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

        installedResources.add(configMap);

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

        installedResources.add(broker);

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
                // .withCloudEventURI(String.format("%s/%s/%s", baseUrl, namespace, RESOURCE_NAME))
                .withServiceAccountName(sa.getMetadata().getName())
                .withNewNamespaceSelector()
                .withMatchNames(namespace)
                .endNamespaceSelector()
                .endSpec()
                .build();

        el = kubernetesFacade.serverSideApply(el);

        installedResources.add(el);

        return el;
    }

    private String obtainElUri(final EventListener el) {
        Callable<String> callable = () -> {
            while (true) {
                try {
                    EventListener elInfo = tektonClient.v1beta1().eventListeners().inNamespace(el.getMetadata().getNamespace())
                            .withName(el.getMetadata().getName()).get();

                    String url = elInfo.getStatus().getAddress().getUrl();

                    if (url != null) {
                        return url;
                    }

                } catch (NullPointerException | ApplicationException e) {
                    // Ignored
                }

                Thread.sleep(2000);
            }
        };

        FutureTask<String> future = new FutureTask<>(callable);

        future.run();

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ApplicationException("Could not find URL for EventListener", e);
        }
    }

    /**
     * Install Knative Trigger responsible for routing events from the Broker into Tekton EventListener.
     */
    private Trigger installKnativeTrigger(Broker broker, EventListener el, String namespace) {
        String uri = obtainElUri(el);

        Trigger trigger = new TriggerBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withBroker(broker.getMetadata().getName())
                .withNewSubscriber()
                .withUri(uri)
                .endSubscriber()
                .endSpec()
                .build();

        trigger = kubernetesFacade.serverSideApply(trigger);

        installedResources.add(trigger);

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
        installedResources.add(roleBinding);

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

        installedResources.add(sa);

        return sa;
    }

    private void installAgogosRoleBindings(List<Map.Entry<String, Set<String>>> bindings) {
        Set<String> processed = new HashSet<>(); // Used to ensure each user ends up with only one role.

        for (Map.Entry<String, Set<String>> e : bindings) {

            String rolebindingName = AGOGOS_ROLE_BINDING_PREFIX + e.getKey();

            if (e.getValue() == null) {
                // Remove rolebinding if no users specified.
                kubernetesFacade.delete(RoleBinding.class, namespace, rolebindingName);
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
                // Remove rolebinding if no users should be it.
                kubernetesFacade.delete(RoleBinding.class, namespace, rolebindingName);
                continue;
            }

            RoleBinding roleBinding = new RoleBindingBuilder()
                    .withNewMetadata()
                    .withName(rolebindingName)
                    .withNamespace(namespace)
                    .withLabels(LABELS)
                    .endMetadata()
                    .withSubjects(subjects)
                    .withNewRoleRef()
                    .withKind(HasMetadata.getKind(ClusterRole.class))
                    .withName(e.getKey())
                    .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
                    .endRoleRef()
                    .build();

            roleBinding = kubernetesFacade.serverSideApply(roleBinding);
            installedResources.add(roleBinding);

            processed.addAll(e.getValue()); // Add all new users as processed.
        }
    }

    private void installAgogosQuota() {
        if (quotaFile == null) {
            kubernetesFacade.delete(ResourceQuota.class, namespace, AGOGOS_QUOTA_NAME);
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

            installedResources.add(resourceQuota);
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
}
