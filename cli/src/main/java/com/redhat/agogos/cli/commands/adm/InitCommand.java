package com.redhat.agogos.cli.commands.adm;

import com.cronutils.utils.StringUtils;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.triggers.v1alpha1.EventListener;
import io.fabric8.tekton.triggers.v1alpha1.EventListenerBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true, name = "init", description = "Initialize selected namespace to work with Agogos")
public class InitCommand implements Runnable {
    private static final String AGOGOS_EL = "agogos-el";
    private static final String EL = "default";
    private static final String KNATIVE_BROKER = "default";

    @Option(names = { "--namespace", "-n" }, required = true, description = "Name of the namespace to be initialized")
    String namespace;

    @Option(names = { "--instance" }, required = true, defaultValue = "agogos", description = "The Agogos instance")
    String agogosNamespace;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    @Inject
    KnativeClient knativeClient;

    private List<HasMetadata> installedResources = new ArrayList<>();

    @Override
    public void run() {
        validate();

        System.out.println(String.format("Initializing '%s' namespace with Agogos resources...", namespace));

        installNamespace();
        installClusterRoleBinding();
        installSa();
        installTektonEl();
        installKnativeTrigger();

        status(installedResources);

        System.out.println(String.format("Done, '%s' namespace initialized and ready to use!", namespace));
    }

    private void validate() {
        Namespace ns = kubernetesClient.namespaces().withName(agogosNamespace).get();

        if (ns == null) {
            throw new ApplicationException(
                    "The '{}' namespace could not be found. Make sure you install Agogos in this namespace before continuing.",
                    agogosNamespace);
        }
    }

    private void installKnativeTrigger() {
        Trigger trigger = new TriggerBuilder().withNewMetadata().withName(EL).endMetadata().withNewSpec()
                .withBroker(KNATIVE_BROKER).withNewSubscriber().withNewRef()
                .withApiVersion(HasMetadata.getApiVersion(EventListener.class))
                .withKind(HasMetadata.getKind(EventListener.class)).withName(EL).withNamespace(namespace).endRef()
                .endSubscriber().endSpec().build();

        knativeClient.triggers().inNamespace(agogosNamespace).createOrReplace(trigger);

        installedResources.add(trigger);
    }

    private void installNamespace() {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();

        ns = kubernetesClient.namespaces().createOrReplace(ns);

        installedResources.add(ns);
    }

    private void installTektonEl() {
        EventListener el = new EventListenerBuilder().withNewMetadata().withName(EL).endMetadata().withNewSpec()
                .withServiceAccountName(AGOGOS_EL).withNewNamespaceSelector().withMatchNames(namespace).endNamespaceSelector()
                .endSpec().build();

        el = tektonClient.v1alpha1().eventListeners().inNamespace(namespace).createOrReplace(el);

        installedResources.add(el);
    }

    private void installClusterRoleBinding() {
        ClusterRoleBinding crb = new ClusterRoleBindingBuilder().withNewMetadata().withName(AGOGOS_EL).endMetadata()
                .withSubjects(
                        new SubjectBuilder().withKind("ServiceAccount").withName(AGOGOS_EL).withNamespace(namespace).build())
                .withNewRoleRef().withApiGroup("rbac.authorization.k8s.io").withKind("ClusterRole").withName(AGOGOS_EL)
                .endRoleRef().build();

        crb = kubernetesClient.rbac().clusterRoleBindings().createOrReplace(crb);

        installedResources.add(crb);
    }

    private void installSa() {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/instance", namespace);
        labels.put("app.kubernetes.io/part-of", "agogos");

        ServiceAccount sa = new ServiceAccountBuilder().withNewMetadata().withName(AGOGOS_EL).withLabels(labels).endMetadata()
                .build();

        sa = kubernetesClient.serviceAccounts().inNamespace(namespace).createOrReplace(sa);

        installedResources.add(sa);
    }

    private void status(List<HasMetadata> k8sResources) {
        StringBuilder sb = new StringBuilder(System.getProperty("line.separator"));

        for (HasMetadata o : k8sResources) {
            String prefix = HasMetadata.getGroup(o.getClass());

            if (StringUtils.isEmpty(prefix)) {
                prefix = o.getKind().toLowerCase();
            }

            sb.append("  -> ").append(prefix).append("/").append(o.getMetadata().getName());

            if (o instanceof Namespaced) {
                sb.append(" (").append(o.getMetadata().getNamespace()).append(")");
            }

            sb.append(System.getProperty("line.separator"));
        }

        System.out.println(sb.toString());
    }
}
