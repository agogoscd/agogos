package com.redhat.agogos.cli;

import com.redhat.agogos.core.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
@ApplicationScoped
public class ResourceLoader {

    @Inject
    private KubernetesSerialization objectMapper;

    Yaml yaml;

    void init(@Observes StartupEvent ev) {
        LoaderOptions opts = new LoaderOptions();
        opts.setMaxAliasesForCollections(200);
        yaml = new Yaml(opts);
    }

    public List<HasMetadata> loadResources(InputStream stream) {
        Iterable<Object> elements = yaml.loadAll(stream);

        List<HasMetadata> resources = new ArrayList<>();
        try {
            elements.forEach(element -> {
                if (element != null) {
                    if (element instanceof List) {
                        ((List<?>) element).forEach(entry -> {
                            resources.add(objectMapper.convertValue(entry, GenericKubernetesResource.class));
                        });
                    } else {
                        resources.add(objectMapper.convertValue(element, GenericKubernetesResource.class));
                    }
                }
            });
        } catch (YAMLException e) {
            throw new ApplicationException("Could not load resources", e);
        }

        return resources;

    }
}
