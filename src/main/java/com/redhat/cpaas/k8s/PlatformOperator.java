package com.redhat.cpaas.k8s;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;

@ApplicationScoped
public class PlatformOperator {
    private static final Logger LOG = Logger.getLogger(PlatformOperator.class);

    @ConfigProperty(name = "kubernetes.namespace")
    String namespace;

    @Inject
    KubernetesClient kubernetesClient;

    Operator operator;

    @PostConstruct
    void init() {
        operator = new Operator(kubernetesClient, DefaultConfigurationService.instance());
    }

    public void registerController(ResourceController controller) {
        LOG.infov("Registering new controller ''{0}'' on namespaces: ''{1}''", controller.getClass().getName(),
                namespace);
        // GenericRetry.defaultLimitedExponentialRetry()
        operator.registerController(controller, namespace);
    }
}
