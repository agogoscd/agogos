package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.KubernetesFacade;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerBuilder;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.EventListenerBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
@RegisterForReflection
public class BrokerInstaller {

    private static final String RESOURCE_NAME = "agogos";
    private static final String RESOURCE_NAME_EVENTING = "agogos-eventing";

    @ConfigProperty(name = "agogos.cloud-events.base-url", defaultValue = "http://broker-ingress.knative-eventing.svc.cluster.local")
    String baseUrl;

    @Inject
    KnativeClient knativeClient;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    TektonClient tektonClient;

    private List<HasMetadata> resources = new ArrayList<>();

    public List<HasMetadata> install(String namespace, Map<String, String> labels) {

        ServiceAccount eventingSa = installEventingSa(namespace, labels);
        installEventingRoleBinding(eventingSa, namespace);
        ConfigMap configMap = installBrokerConfig(namespace);
        EventListener el = installTektonEl(eventingSa, namespace);
        Broker broker = installKnativeBroker(configMap, namespace, labels);
        installKnativeTrigger(broker, el, namespace);

        return resources;
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

        resources.add(configMap);

        return configMap;
    }

    private Broker installKnativeBroker(ConfigMap configuration, String namespace, Map<String, String> labels) {
        Broker broker = new BrokerBuilder()
                .withNewMetadata()
                .withName(RESOURCE_NAME)
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

        broker = knativeClient.brokers().inNamespace(namespace).resource(broker).serverSideApply();

        resources.add(broker);

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
                .endMetadata()
                .withNewSpec()
                // .withCloudEventURI(String.format("%s/%s/%s", baseUrl, namespace, RESOURCE_NAME))
                .withServiceAccountName(sa.getMetadata().getName())
                .withNewNamespaceSelector()
                .withMatchNames(namespace)
                .endNamespaceSelector()
                .endSpec()
                .build();

        el = tektonClient.v1beta1().eventListeners().inNamespace(namespace).resource(el).serverSideApply();

        resources.add(el);

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
                .endMetadata()
                .withNewSpec()
                .withBroker(broker.getMetadata().getName())
                .withNewSubscriber()
                .withUri(uri)
                .endSubscriber()
                .endSpec()
                .build();

        trigger = knativeClient.triggers().inNamespace(namespace).resource(trigger).serverSideApply();

        resources.add(trigger);

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
        resources.add(roleBinding);

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

        resources.add(sa);

        return sa;
    }
}
