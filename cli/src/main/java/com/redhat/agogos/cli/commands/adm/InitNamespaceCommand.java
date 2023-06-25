package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.AbstractCommand;
import com.redhat.agogos.cli.commands.adm.install.BrokerInstaller;
import com.redhat.agogos.cli.commands.adm.install.CoreInstaller;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import jakarta.inject.Inject;
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
import java.util.stream.Collectors;

@Command(mixinStandardHelpOptions = true, name = "init-namespace", aliases = {
        "init" }, description = "Initialize selected namespace to work with Agogos")
public class InitNamespaceCommand extends AbstractCommand {

    @ConfigProperty(name = "agogos.cloud-events.base-url", defaultValue = "http://broker-ingress.knative-eventing.svc.cluster.local")
    String baseUrl;

    @Inject
    BrokerInstaller brokerInstaller;

    private static final Logger LOG = LoggerFactory.getLogger(InitNamespaceCommand.class);

    private static final String AGOGOS_QUOTA_NAME = "agogos-quota";
    private static final String AGOGOS_ROLE_BINDING_PREFIX = "agogos-";
    private static final String RESOURCE_NAME = "agogos";
    private static final String RESOURCE_NAME_CONFIG = "agogos-config";

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
        LOG.info("Initializing '{}' namespace with Agogos resources...", namespace);

        installNamespace();
        installConfig();

        ServiceAccount sa = installMainSa();
        installMainRoleBinding(sa);

        installedResources.addAll(brokerInstaller.install(namespace, LABELS));

        List<Map.Entry<String, Set<String>>> bindings = Arrays.asList(
                new AbstractMap.SimpleEntry<String, Set<String>>("admin", admin),
                new AbstractMap.SimpleEntry<String, Set<String>>("edit", editor),
                new AbstractMap.SimpleEntry<String, Set<String>>("view", viewer));
        installAgogosRoleBindings(bindings);

        installAgogosQuota();

        Helper.status(installedResources);

        LOG.info("Done, '{}' namespace initialized and ready to use!", namespace);
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
                .endMetadata()
                .withData(data)
                .build();

        cm = kubernetesClient.configMaps().inNamespace(namespace).resource(cm).serverSideApply();

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

        ns = kubernetesClient.namespaces().resource(ns).serverSideApply();

        installedResources.add(ns);
    }

    private void installMainRoleBinding(ServiceAccount sa) {
        RoleBinding roleBinding = new RoleBindingBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
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

        roleBinding = kubernetesClient.rbac().roleBindings().inNamespace(namespace).resource(roleBinding).serverSideApply();

        installedResources.add(roleBinding);
    }

    /**
     * Installs ServiceAccount used by Pipelines.
     */
    private ServiceAccount installMainSa() {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
                .withLabels(LABELS)
                .endMetadata()
                .build();

        sa = kubernetesClient.serviceAccounts().inNamespace(namespace).resource(sa).serverSideApply();

        installedResources.add(sa);

        return sa;
    }

    private void installAgogosRoleBindings(List<Map.Entry<String, Set<String>>> bindings) {
        Set<String> processed = new HashSet<>(); // Used to ensure each user ends up with only one role.

        for (Map.Entry<String, Set<String>> e : bindings) {

            String rolebindingName = AGOGOS_ROLE_BINDING_PREFIX + e.getKey();

            if (e.getValue() == null) {
                // Remove rolebinding if no users specified.
                kubernetesClient.rbac().roleBindings().inNamespace(namespace).withName(rolebindingName).delete();
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
                kubernetesClient.rbac().roleBindings().inNamespace(namespace).withName(rolebindingName).delete();
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

            roleBinding = kubernetesClient.rbac().roleBindings().inNamespace(namespace).resource(roleBinding).serverSideApply();

            installedResources.add(roleBinding);

            processed.addAll(e.getValue()); // Add all new users as processed.
        }
    }

    private void installAgogosQuota() {
        if (quotaFile == null) {
            // Remove any existing quota.
            kubernetesClient.resourceQuotas().inNamespace(namespace).withName(AGOGOS_QUOTA_NAME).delete();
            return;
        }

        try {
            ResourceQuota resourceQuota = Serialization.unmarshal(new FileInputStream(quotaFile), ResourceQuota.class);

            resourceQuota.getMetadata().setNamespace(namespace);
            resourceQuota.getMetadata().setName(AGOGOS_QUOTA_NAME);

            resourceQuota = kubernetesClient.resourceQuotas().inNamespace(namespace).resource(resourceQuota).serverSideApply();

            installedResources.add(resourceQuota);
        } catch (FileNotFoundException e) {
            LOG.error("File " + quotaFile.getName() + " not found, no resource quota applied", e);
        }
    }

    private boolean isAgogosCoreNamespace() {
        Namespace ns = kubernetesClient.namespaces().withName(namespace).get();
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
