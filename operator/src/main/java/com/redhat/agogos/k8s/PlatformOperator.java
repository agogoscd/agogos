package com.redhat.agogos.k8s;

import com.redhat.agogos.k8s.controllers.ComponentController;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class PlatformOperator {
    private static final Logger LOG = Logger.getLogger(PlatformOperator.class);

    // we inject a specific configuration service instance that provides a plain ObjectMapper
    // instead of using the one from Fabric8 to work around
    // https://github.com/fabric8io/kubernetes-client/issues/5009
    @Inject
    AgogosConfigurationServiceProvider.AgogosConfigurationService configuration;

    /**
     * Required to start the operator and register all controllers automatically.
     */
    @Inject
    Operator operator;

    @Inject
    ComponentController componentController;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting operator");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Stopping operator");
    }
}
