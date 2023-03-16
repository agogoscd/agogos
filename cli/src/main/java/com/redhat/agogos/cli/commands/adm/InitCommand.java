package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.commands.adm.install.CoreInstaller;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerBuilder;
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
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
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

    private static final String RESOURCE_NAME = "agogos";
    private static final String RESOURCE_NAME_EVENTING = "agogos-eventing";
    private static final String RESOURCE_NAME_CONFIG = "agogos-config";

    private static final Map<String, String> LABELS = Map.of(//
            "app.kubernetes.io/instance", "default", //
            "app.kubernetes.io/part-of", "agogos", //
            "app.kubernetes.io/component", "core"//
    );

    @Option(names = { "--namespace", "-n" }, required = true, description = "Name of the namespace to be initialized")
    String namespace;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    @Inject
    KnativeClient knativeClient;

    private List<HasMetadata> installedResources = new ArrayList<>();

    @Override
    public void run() {
        LOG.info("Initializing '{}' namespace with Agogos resources...", namespace);

        installNamespace();

        ServiceAccount sa = installMainSa();
        ServiceAccount eventingSa = installEventingSa();

        installMainRoleBinding(sa);
        installEventingRoleBinding(eventingSa);

        installConfig();
        ConfigMap brokerConfig = installBrokerConfig();

        EventListener el = installTektonEl(eventingSa);
        Broker broker = installKnativeBroker(brokerConfig);

        installKnativeTrigger(broker, el);

        Helper.status(installedResources);

        LOG.info("Done, '{}' namespace initialized and ready to use!", namespace);
    }

    private ConfigMap installBrokerConfig() {
        ConfigMap configMap = new ConfigMapBuilder() //
                .withNewMetadata() //
                .withName("agogos-broker-config") //
                .endMetadata() //
                .withData(Map.of("channelTemplateSpec", "apiVersion: messaging.knative.dev/v1\nkind: InMemoryChannel"))
                .build();

        configMap = kubernetesClient.configMaps().inNamespace(namespace).resource(configMap).createOrReplace();

        installedResources.add(configMap);

        return configMap;
    }

    private Broker installKnativeBroker(ConfigMap configuration) {
        Broker broker = new BrokerBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME) //
                .withLabels(LABELS) //
                .withAnnotations(Map.of("eventing.knative.dev/broker.class", "MTChannelBasedBroker")) // TODO: Not good for production deployment, fine for now
                .endMetadata() //
                .withNewSpec() //
                .withNewConfig() //
                .withApiVersion(configuration.getApiVersion()) //
                .withKind(configuration.getKind()) //
                .withName(configuration.getMetadata().getName()) //
                .withNamespace(configuration.getMetadata().getNamespace()) //
                .endConfig() //
                .endSpec() //
                .build();

        broker = knativeClient.brokers().inNamespace(namespace).resource(broker).createOrReplace();

        installedResources.add(broker);

        return broker;
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
        String exampleData = new StringBuilder() //
                .append("# This content is not used and is provided as an example.") //
                .append(System.getProperty("line.separator")) //
                .append("# Please refer to Agogos Stage and Builder documentation.") //
                .toString();

        Map<String, String> data = Map.of("_example", exampleData);

        ConfigMap cm = new ConfigMapBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME_CONFIG) //
                .endMetadata() //
                .withData(data) //
                .build();

        cm = kubernetesClient.configMaps().inNamespace(namespace).resource(cm).createOrReplace();

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

    private String obtainElUri(final EventListener el) {
        Callable<String> callable = () -> {
            while (true) {
                try {
                    EventListener elInfo = tektonClient.v1alpha1().eventListeners().inNamespace(el.getMetadata().getNamespace())
                            .withName(el.getMetadata().getName()).get();

                    String url = elInfo.getStatus().getAddress().getUrl();

                    if (url != null) {
                        return url;
                    }

                    throw new ApplicationException("Could not find URL for EventListener");
                } catch (NullPointerException | ApplicationException e) {
                    // Ignored
                }

                Thread.sleep(2000);

                throw new ApplicationException("Could not read EventListener's '{}/{}' url", el.getMetadata().getNamespace(),
                        el.getMetadata().getName());
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
    private void installKnativeTrigger(Broker broker, EventListener el) {
        String uri = obtainElUri(el);

        Trigger trigger = new TriggerBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME) //
                .endMetadata() //
                .withNewSpec() //
                .withBroker(broker.getMetadata().getName()) //
                .withNewSubscriber() //
                .withUri(uri) //
                .endSubscriber() //
                .endSpec() //
                .build();

        trigger = knativeClient.triggers().inNamespace(namespace).resource(trigger).createOrReplace();

        installedResources.add(trigger);
    }

    /**
     * <p>
     * Prepares the namespace for the new project.
     * </p>
     */
    private void installNamespace() {
        Namespace ns = new NamespaceBuilder() //
                .withNewMetadata() //
                .withName(namespace) //
                .withLabels(LABELS) //
                .endMetadata() //
                .build();

        ns = kubernetesClient.namespaces().resource(ns).createOrReplace();

        installedResources.add(ns);
    }

    /**
     * <p>
     * Prepares Tekton {@link EventListener} responsible for handling CloudEvents coming from the broker.
     * </p>
     */
    private EventListener installTektonEl(ServiceAccount sa) {
        EventListener el = new EventListenerBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME) //
                .endMetadata() //
                .withNewSpec() //
                .withServiceAccountName(sa.getMetadata().getName()) //
                .withNewNamespaceSelector() //
                .withMatchNames(namespace) //
                .endNamespaceSelector() //
                .endSpec() //
                .build();

        el = tektonClient.v1alpha1().eventListeners().inNamespace(namespace).resource(el).createOrReplace();

        installedResources.add(el);

        return el;
    }

    /**
     * Ensure the {@link ClusterRoleBinding} for the {@link ServiceAccount} used by the Tekton {@link EventListener} exists and
     * is configured properly.
     * 
     */
    private ClusterRoleBinding installEventingRoleBinding(ServiceAccount sa) {
        ClusterRoleBinding roleBinding = new ClusterRoleBindingBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME_EVENTING) //
                .endMetadata()
                .withSubjects(
                        new SubjectBuilder() //
                                .withApiGroup(HasMetadata.getGroup(sa.getClass())) //
                                .withKind(sa.getKind()) //
                                .withName(sa.getMetadata().getName()) //
                                .withNamespace(namespace) //
                                .build())
                .withNewRoleRef() //
                .withApiGroup(HasMetadata.getGroup(ClusterRole.class)) //
                .withKind(HasMetadata.getKind(ClusterRole.class)) //
                .withName(RESOURCE_NAME_EVENTING) //
                .endRoleRef() //
                .build();

        roleBinding = kubernetesClient.rbac().clusterRoleBindings().resource(roleBinding).createOrReplace();

        installedResources.add(roleBinding);

        return roleBinding;
    }

    private void installMainRoleBinding(ServiceAccount sa) {
        RoleBinding roleBinding = new RoleBindingBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME) //
                .endMetadata() //
                .addNewSubject() //
                .withApiGroup(HasMetadata.getGroup(sa.getClass())) //
                .withKind(sa.getKind()) //
                .withName(sa.getMetadata().getName()) //
                .endSubject() //
                .withNewRoleRef() //
                .withKind(HasMetadata.getKind(ClusterRole.class)) //
                .withName(CoreInstaller.CLUSTER_ROLE_VIEW_NAME) //
                .withApiGroup(HasMetadata.getGroup(ClusterRole.class)) //
                .endRoleRef() //
                .build();

        roleBinding = kubernetesClient.rbac().roleBindings().inNamespace(namespace).resource(roleBinding).createOrReplace();

        installedResources.add(roleBinding);
    }

    /**
     * Installs ServiceAccount used by Pipelines.
     */
    private ServiceAccount installMainSa() {
        ServiceAccount sa = new ServiceAccountBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME) //
                .withLabels(LABELS) //
                .endMetadata() //
                .build();

        sa = kubernetesClient.serviceAccounts().inNamespace(namespace).resource(sa).createOrReplace();

        installedResources.add(sa);

        return sa;
    }

    private ServiceAccount installEventingSa() {
        ServiceAccount sa = new ServiceAccountBuilder() //
                .withNewMetadata() //
                .withName(RESOURCE_NAME_EVENTING) //
                .withLabels(LABELS) //
                .endMetadata() //
                .build();

        sa = kubernetesClient.serviceAccounts().inNamespace(namespace).resource(sa).createOrReplace();

        installedResources.add(sa);

        return sa;
    }

}
