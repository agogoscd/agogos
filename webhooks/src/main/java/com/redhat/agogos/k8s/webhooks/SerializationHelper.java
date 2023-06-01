package com.redhat.agogos.k8s.webhooks;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Handler;
import com.redhat.agogos.v1alpha1.Run;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SerializationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SerializationHelper.class);

    @Inject
    Instance<AdmissionHandler<? extends CustomResource<?, ?>>> handlers;

    /**
     * <p>
     * Method to register all CustomResource's that are used in validation and
     * admission webhooks.
     * </p>
     *
     * <p>
     * This is required to make it possible to deserialize object incoming with the
     * requests.
     * </p>
     *
     * @param ev Startup event (ignored)
     */
    void registerResources(@Observes StartupEvent ev) {

        LOG.debug("Registering CustomResources with Kubernetes");

        @SuppressWarnings("unchecked")
        Class<? extends KubernetesResource>[] classes = new Class[] { Build.class, Component.class, Handler.class, Run.class,
                Trigger.class };
        for (Class<? extends KubernetesResource> clazz : classes) {

            LOG.debug("Registering '{}' CustomResource with Kubernetes deserializer", clazz.getName());

            KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(clazz), HasMetadata.getKind(clazz),
                    clazz);
        }
    }
}
