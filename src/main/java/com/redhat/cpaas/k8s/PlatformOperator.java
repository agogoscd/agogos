package com.redhat.cpaas.k8s;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class PlatformOperator {
    private static final Logger LOG = Logger.getLogger(PlatformOperator.class);

    /**
     * Required to start the operator and register all controllers automatically.
     */
    @Inject
    Operator operator;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting operator");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Stopping operator");
    }
}
