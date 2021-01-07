package com.redhat.cpaas.k8s;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.cpaas.k8s.controllers.ComponentController;

import org.jboss.logging.Logger;

import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class PlatformOperator {
    private static final Logger LOG = Logger.getLogger(PlatformOperator.class);

    @Inject
    Operator operator;

    @Inject
    ConfigurationService configuration;

    @Inject
    ComponentController componentController;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Starting operator");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Stopping operator");
    }
}
