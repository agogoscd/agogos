package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.install.CoreInstaller;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.triggers.v1alpha1.EventListener;
import io.fabric8.tekton.triggers.v1alpha1.EventListenerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Command(mixinStandardHelpOptions = true, name = "init", description = "Initialize selected namespace to work with Agogos")
public class InitCommand implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(InitCommand.class);

    private static final String CONFIG_MAP_NAME = "agogos-config";

    // ServiceAccount
    private static final String SERVICE_ACCOUNT_TEKTON_EL_NAME = "agogos-el";
    private static final String SERVICE_ACCOUNT_NAME = "agogos";

    // Tekton EventListener
    private static final String TEKTON_EVENT_LISTENER_NAME = "agogos";

    // Knative Trigger
    private static final String KNATIVE_TRIGGER_NAME = "default";

    // Knative Broker
    private static final String KNATIVE_BROKER_NAME = "default";

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

        LOG.info("Initializing '{}' namespace with Agogos resources...", namespace);

        installNamespace();
        installClusterRoleBinding();
        installSa();
        installCm();
        installTektonElSa();
        EventListener el = installTektonEl();
        installKnativeTrigger(el);

        Helper.status(installedResources);

        LOG.info("Done, '{}' namespace initialized and ready to use!", namespace);
    }

    /**
     * Checks whether Agogos is installed.
     */
    private void validate() {
        Namespace ns = kubernetesClient.namespaces().withName(agogosNamespace).get();

        if (ns == null) {
            throw new ApplicationException(
                    "The '{}' namespace could not be found. Make sure you install Agogos in this namespace before continuing.",
                    agogosNamespace);
        }
    }

    private void installCm() {
        String exampleData = new StringBuilder() //
                .append("# This content is not used and is provided as an example.") //
                .append(System.getProperty("line.separator")) //
                .append("# Please refer to Agogos Stage and Builder documentation.") //
                .toString();

        Map<String, String> data = Map.of("_example", exampleData);

        ConfigMap cm = new ConfigMapBuilder() //
                .withNewMetadata() //
                .withName(CONFIG_MAP_NAME) //
                .endMetadata() //
                .withData(data) //
                .build();

        cm = kubernetesClient.configMaps().inNamespace(namespace).createOrReplace(cm);

        installedResources.add(cm);
    }

    static class ReadUrlTask implements Callable<String> {
        EventListener eventListener;
        TektonClient tektonClient;

        ReadUrlTask(TektonClient tektonClient, EventListener eventListener) {
            this.tektonClient = tektonClient;
            this.eventListener = eventListener;
        }

        @Override
        public String call() throws Exception {
            tektonClient.v1alpha1().eventListeners().inNamespace(eventListener.getMetadata().getNamespace())
                    .withName(eventListener.getMetadata().getName()).get();

            String url = eventListener.getStatus().getAddress().getUrl();

            if (url != null) {
                return url;
            }

            throw new ApplicationException("EventListener's '{}/{}' url is null", eventListener.getMetadata().getNamespace(),
                    eventListener.getMetadata().getName());
        }

    }

    private String readElUrl() {
        EventListener el = tektonClient.v1alpha1().eventListeners().inNamespace(namespace)
                .withName(TEKTON_EVENT_LISTENER_NAME).get();

        String url = el.getStatus().getAddress().getUrl();

        if (url != null) {
            return url;
        }

        throw new ApplicationException("Could not find URL for EventListener");
    }

    private String obtainElUrl() {
        Callable<String> callable = () -> {
            while (true) {
                try {
                    return readElUrl();
                } catch (NullPointerException | ApplicationException e) {
                    // Ignored
                }

                Thread.sleep(2000);

                throw new ApplicationException("Could not read EventListener's '{}/{}' url", namespace,
                        TEKTON_EVENT_LISTENER_NAME);
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
    private void installKnativeTrigger(EventListener el) {
        Trigger trigger = new TriggerBuilder() //
                .withNewMetadata() //
                .withName(KNATIVE_TRIGGER_NAME) //
                .endMetadata() //
                .withNewSpec() //
                .withBroker(KNATIVE_BROKER_NAME) //
                .withNewSubscriber() //
                .withUri(obtainElUrl()) //
                .endSubscriber() //
                .endSpec() //
                .build();

        trigger = knativeClient.triggers().inNamespace(agogosNamespace).createOrReplace(trigger);

        installedResources.add(trigger);
    }

    /**
     * Prepares the namespace for new product.
     */
    private void installNamespace() {
        Namespace ns = new NamespaceBuilder() //
                .withNewMetadata() //
                .withName(namespace) //
                .endMetadata() //
                .build();

        ns = kubernetesClient.namespaces().createOrReplace(ns);

        installedResources.add(ns);
    }

    /**
     * Prepares Tekton EventListener responsible for handling CloudEvents coming from the broker.
     */
    private EventListener installTektonEl() {
        EventListener el = new EventListenerBuilder() //
                .withNewMetadata()//
                .withName(TEKTON_EVENT_LISTENER_NAME) //
                .endMetadata() //
                .withNewSpec() //
                .withServiceAccountName(SERVICE_ACCOUNT_TEKTON_EL_NAME) //
                .withNewNamespaceSelector() //
                .withMatchNames(namespace) //
                .endNamespaceSelector()//
                .endSpec() //
                .build();

        el = tektonClient.v1alpha1().eventListeners().inNamespace(namespace).createOrReplace(el);

        installedResources.add(el);

        return el;
    }

    /**
     * Ensure the ClusterRoleBinding for the ServiceAccount used by the Tekton EventListener exists and is configured properly.
     */
    private void installClusterRoleBinding() {
        String name = new StringBuilder() //
                .append(SERVICE_ACCOUNT_TEKTON_EL_NAME) //
                .append("-") //
                .append(namespace) //
                .toString();

        ClusterRoleBinding crb = new ClusterRoleBindingBuilder() //
                .withNewMetadata() //
                .withName(name) //
                .endMetadata()
                .withSubjects(
                        new SubjectBuilder() //
                                .withKind(HasMetadata.getKind(ServiceAccount.class)) //
                                .withName(SERVICE_ACCOUNT_TEKTON_EL_NAME) //
                                .withNamespace(namespace) //
                                .build())
                .withNewRoleRef() //
                .withApiGroup(HasMetadata.getGroup(ClusterRole.class)) //
                .withKind(HasMetadata.getKind(ClusterRole.class)) //
                .withName(CoreInstaller.CLUSTER_ROLE_NAME_EVENTING) //
                .endRoleRef() //
                .build();

        crb = kubernetesClient.rbac().clusterRoleBindings().createOrReplace(crb);

        installedResources.add(crb);
    }

    /**
     * Installs ServiceAccount used by Pipelines.
     */
    private void installSa() {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/instance", namespace);
        labels.put("app.kubernetes.io/part-of", "agogos");

        ServiceAccount sa = new ServiceAccountBuilder() //
                .withNewMetadata() //
                .withName(SERVICE_ACCOUNT_NAME) //
                .withLabels(labels) //
                .endMetadata() //
                .build();

        sa = kubernetesClient.serviceAccounts().inNamespace(namespace).createOrReplace(sa);

        installedResources.add(sa);
    }

    private void installTektonElSa() {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/instance", namespace);
        labels.put("app.kubernetes.io/part-of", "agogos");

        ServiceAccount sa = new ServiceAccountBuilder() //
                .withNewMetadata() //
                .withName(SERVICE_ACCOUNT_TEKTON_EL_NAME) //
                .withLabels(labels) //
                .endMetadata() //
                .build();

        sa = kubernetesClient.serviceAccounts().inNamespace(namespace).createOrReplace(sa);

        installedResources.add(sa);
    }

}
