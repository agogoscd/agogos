package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Priority
public abstract class Installer {
    private static final Logger LOG = LoggerFactory.getLogger(Installer.class);

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "resources.wait")
    boolean waitForResources;

    public abstract void install(InstallProfile profile, String namespace);

    private InputStream urlToStream(String url) {
        URL resource = null;

        try {
            resource = new URL(url);
        } catch (MalformedURLException e) {
            throw new ApplicationException("Could not parse resource url: {}", url, e);
        }

        try {
            return resource.openStream();
        } catch (IOException e) {
            throw new ApplicationException("Could not load resource from url: {}", url, e);
        }
    }

    private List<HasMetadata> loadKubernetesResources(InputStream stream) {
        List<HasMetadata> resources = null;

        try {
            resources = kubernetesClient.load(stream).get();
        } catch (KubernetesClientException e) {
            throw new ApplicationException("Could not read resources", e);
        }

        return resources;
    }

    protected List<HasMetadata> installKubernetesResources(String url, String namespace) {
        return installKubernetesResources(urlToStream(url), namespace);
    }

    protected List<HasMetadata> installKubernetesResources(String url, String namespace, Consumer<List<HasMetadata>> consumer) {
        return installKubernetesResources(urlToStream(url), namespace, consumer);
    }

    protected List<HasMetadata> installKubernetesResources(InputStream stream, String namespace) {
        return installKubernetesResources(loadKubernetesResources(stream), namespace);
    }

    protected List<HasMetadata> installKubernetesResources(InputStream stream, String namespace,
            Consumer<List<HasMetadata>> consumer) {
        return installKubernetesResources(loadKubernetesResources(stream), namespace, consumer);
    }

    protected List<HasMetadata> installKubernetesResources(List<HasMetadata> resources, String namespace) {
        return installKubernetesResources(resources, namespace, null);
    }

    protected List<HasMetadata> installKubernetesResources(List<HasMetadata> resources, String namespace,
            Consumer<List<HasMetadata>> consumer) {
        if (consumer != null) {
            consumer.accept(resources);
        }

        if (waitForResources) {
            try {
                resources = kubernetesClient.resourceList(resources).inNamespace(namespace).createOrReplaceAnd()
                        .withWaitRetryBackoff(5, TimeUnit.SECONDS, 2).waitUntilReady(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new ApplicationException("Could not load resources", e);
            }
        } else {
            resources = kubernetesClient.resourceList(resources).inNamespace(namespace).createOrReplace();
        }

        return resources;
    }

    protected void status(List<HasMetadata> resources) {
        status(resources.toArray(new HasMetadata[resources.size()]));
    }

    protected void status(HasMetadata... resources) {
        StringBuilder sb = null;

        for (HasMetadata resource : resources) {
            sb = new StringBuilder();

            String prefix = HasMetadata.getGroup(resource.getClass());

            if (StringUtils.isEmpty(prefix)) {
                prefix = resource.getKind().toLowerCase();
            }

            sb.append("👉  Done: ").append(prefix).append("/").append(HasMetadata.getPlural(resource.getClass())).append("/")
                    .append(resource.getMetadata().getName());

            if (resource instanceof Namespaced) {
                sb.append(" (").append(resource.getMetadata().getNamespace()).append(")");
            }

            LOG.debug(sb.toString());
        }
    }
}
