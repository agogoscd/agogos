package com.redhat.agogos.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Config;
import io.javaoperatorsdk.operator.api.config.*;
import io.javaoperatorsdk.operator.api.monitoring.Metrics;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResourceFactory;
import io.javaoperatorsdk.operator.processing.dependent.workflow.ManagedWorkflowFactory;
import io.quarkiverse.operatorsdk.runtime.QuarkusConfigurationService;
import io.quarkiverse.operatorsdk.runtime.QuarkusControllerConfiguration;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class AgogosConfigurationServiceProvider {
    @Inject
    QuarkusConfigurationService qosdkConfigurationService;

    @Produces
    @Singleton
    AgogosConfigurationService congfigurationService() {
        return new AgogosConfigurationService(qosdkConfigurationService);
    }

    public static class AgogosConfigurationService implements ConfigurationService {

        private final QuarkusConfigurationService delegate;
        private final ObjectMapper mapper;
        private final Cloner cloner;

        @Override
        public Config getClientConfiguration() {
            return delegate.getClientConfiguration();
        }

        @Override
        public <R extends HasMetadata> QuarkusControllerConfiguration<R> getConfigurationFor(Reconciler<R> reconciler) {
            return delegate.getConfigurationFor(reconciler);
        }

        @Override
        public boolean checkCRDAndValidateLocalModel() {
            return delegate.checkCRDAndValidateLocalModel();
        }

        @Override
        public int concurrentReconciliationThreads() {
            return delegate.concurrentReconciliationThreads();
        }

        @Override
        public int getTerminationTimeoutSeconds() {
            return delegate.getTerminationTimeoutSeconds();
        }

        @Override
        public Metrics getMetrics() {
            return delegate.getMetrics();
        }

        @Override
        public Optional<LeaderElectionConfiguration> getLeaderElectionConfiguration() {
            return delegate.getLeaderElectionConfiguration();
        }

        @Override
        public Optional<InformerStoppedHandler> getInformerStoppedHandler() {
            return delegate.getInformerStoppedHandler();
        }

        @Override
        public int concurrentWorkflowExecutorThreads() {
            return delegate.concurrentWorkflowExecutorThreads();
        }

        @Override
        public boolean closeClientOnStop() {
            return delegate.closeClientOnStop();
        }

        @Override
        public boolean stopOnInformerErrorDuringStartup() {
            return delegate.stopOnInformerErrorDuringStartup();
        }

        @Override
        public Duration cacheSyncTimeout() {
            return delegate.cacheSyncTimeout();
        }

        @Override
        public DependentResourceFactory<QuarkusControllerConfiguration<?>> dependentResourceFactory() {
            return delegate.dependentResourceFactory();
        }

        @Override
        public ManagedWorkflowFactory<QuarkusControllerConfiguration<?>> getWorkflowFactory() {
            return delegate.getWorkflowFactory();
        }

        @Override
        public Set<String> getKnownReconcilerNames() {
            return delegate.getKnownReconcilerNames();
        }

        @Override
        public Version getVersion() {
            return delegate.getVersion();
        }

        @Override
        public Cloner getResourceCloner() {
            return cloner;
        }

        @Override
        public ObjectMapper getObjectMapper() {
            return mapper;
        }

        @Override
        public ExecutorService getExecutorService() {
            return delegate.getExecutorService();
        }

        @Override
        public ExecutorService getWorkflowExecutorService() {
            return delegate.getWorkflowExecutorService();
        }

        @Override
        public ResourceClassResolver getResourceClassResolver() {
            return delegate.getResourceClassResolver();
        }

        public AgogosConfigurationService(QuarkusConfigurationService delegate) {
            this.delegate = delegate;
            mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            cloner = new Cloner() {
                @Override
                @SuppressWarnings("unchecked")
                public <R extends HasMetadata> R clone(R r) {
                    try {
                        return (R) mapper.readValue(mapper.writeValueAsString(r), r.getClass());
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
        }
    }
}
