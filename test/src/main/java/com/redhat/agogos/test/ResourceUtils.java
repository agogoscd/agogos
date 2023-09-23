package com.redhat.agogos.test;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.Build;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResourceUtils {

    @Inject
    KubernetesSerialization objectMapper;

    Yaml yaml;

    public ResourceUtils() {
        LoaderOptions opts = new LoaderOptions();
        opts.setMaxAliasesForCollections(200);
        yaml = new Yaml(opts);
    }

    public String testResourceAsString(String path) {
        try {
            return new String(testResourceAsInputStream(path).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream testResourceAsInputStream(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    public List<String> testResourceAsStringList(String path) {
        return new BufferedReader(new InputStreamReader(testResourceAsInputStream(path), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.toList());
    }

    public List<Build> loadTestResources(Class<Build> clazz, String path) {
        Iterable<Object> elements = yaml.loadAll(testResourceAsInputStream(path));

        List<Build> resources = new ArrayList<>();
        try {
            elements.forEach(element -> {
                if (element != null) {
                    if (element instanceof List) {
                        ((List<?>) element).forEach(entry -> {
                            resources.add(objectMapper.convertValue(entry, Build.class));
                        });
                    } else {
                        resources.add(objectMapper.convertValue(element, Build.class));
                    }
                }
            });
        } catch (YAMLException e) {
            throw new ApplicationException("Could not load resources", e);
        }
        return resources;
    }

}
