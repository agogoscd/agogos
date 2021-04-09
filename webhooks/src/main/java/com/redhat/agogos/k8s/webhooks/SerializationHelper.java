package com.redhat.agogos.k8s.webhooks;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import java.lang.reflect.ParameterizedType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
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
    @SuppressWarnings("unchecked")
    void registerResources(@Observes StartupEvent ev) {

        LOG.debug("Registering CustomResources with Kubernetes");

        for (AdmissionHandler<? extends CustomResource<?, ?>> handler : handlers) {
            Class<CustomResource<?, ?>> clazz = (Class<CustomResource<?, ?>>) ((ParameterizedType) handler.getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0];

            LOG.debug("Registering '{}' CustomResource with Kubernetes deserializer", clazz.getName());

            KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(clazz), HasMetadata.getKind(clazz),
                    clazz);
        }
    }
}
