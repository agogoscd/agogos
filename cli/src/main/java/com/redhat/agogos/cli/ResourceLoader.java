package com.redhat.agogos.cli;

import com.redhat.agogos.KubernetesFacade;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.internal.GenericKubernetesResourceOperationsImpl;
import io.fabric8.kubernetes.client.dsl.internal.OperationContext;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.kubernetes.client.vertx.VertxHttpClientFactory;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RegisterForReflection
@ApplicationScoped
public class ResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(Helper.class);

    private static final Integer MAX_RETRIES = 10;
    private static final Integer MAX_INTERVAL = 3;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    KubernetesSerialization objectMapper;

    /**
     * <p>
     * A {@link Map} containing information about resource types known to the Kubernetes cluster.
     * </p>
     * 
     * <p>
     * The key in the map is a String in following format:
     * </p>
     * 
     * <pre>
     * {@code
     * [GROUP]/[VERSION]/[KIND]
     * }
     * </pre>
     * 
     * <p>
     * The value is {@link ResourceMapping} object containing important data such as endpoints for the particular resource.
     * </p>
     * 
     */
    Map<String, ResourceMapping> resourceMappings = new HashMap<>();

    Instant lastResourceMappingUpdate = null;

    /**
     * Used for connecting to the Kubernetes API directly.
     */
    HttpClient httpClient;

    void init(@Observes StartupEvent ev) {
        // Prepare the http client based on the Kubernetes client configuration
        httpClient = new VertxHttpClientFactory().newBuilder(kubernetesFacade.getConfiguration()).build();
    }

    @SuppressWarnings("unchecked")
    private void readMappings() {
        withOpenApi(file -> {
            OpenApi3 openApi = null;

            try {
                // Read and parse the OpenAPI schema (but don't validate it!)
                openApi = new OpenApi3Parser().parse(file, false);
            } catch (ResolutionException | ValidationException e) {
                throw new ApplicationException(
                        "Could not parse OpenAPI information received from Kubernetes.", e);
            }

            openApi.getPaths().entrySet().forEach(entry -> {
                Path path = entry.getValue();

                // Check whether the path supports GET calls
                if (path.hasOperation("get")) {

                    Map<String, Object> extensions = path.getGet().getExtensions();

                    if (extensions != null) {
                        Map<String, String> gvk = (Map<String, String>) extensions.get("x-kubernetes-group-version-kind");

                        if (gvk != null) {

                            String group = gvk.get("group");
                            String version = gvk.get("version");
                            String kind = gvk.get("kind");

                            String key = new StringBuilder().append(group).append("/").append(version).append("/")
                                    .append(kind).toString();

                            ResourceMapping mapping = resourceMappings.computeIfAbsent(key, (val) -> new ResourceMapping());

                            mapping.setGroup(group);
                            mapping.setVersion(version);
                            mapping.setKind(kind);
                            mapping.getEndpoints().add(entry.getKey());
                        }
                    }
                }
            });
        });

        lastResourceMappingUpdate = Instant.now();
    }

    /**
     * <p>
     * Reads the OpenAPI schema from the Kubernetes server, stores it in a temporary file
     * and exposes it to the consumer.
     * </p>
     * 
     * <p>
     * Temporary file is removed after the consumer finishes its task.
     * </p>
     * 
     * @see Consumer
     * 
     */
    private void withOpenApi(Consumer<File> consumer) {
        URL openApiUrl = openApiUrl();
        java.nio.file.Path tempFile = null;

        // Create a temporary file so that we can store OpenAPI information there.
        // This is required so that the OpenAPI parser can read it. Unfortunately
        // it supports only URL (which don't want to use due to authentication configuration)
        // and File objects.
        try {
            tempFile = Files.createTempFile("agogos", "openapi");
        } catch (IOException e) {
            throw new ApplicationException(
                    "Could not create temporary file. Unexpected error occurred.", e);
        }

        final HttpResponse<String> response;

        try {
            // Fetch OpenAPI information
            response = httpClient.sendAsync(httpClient.newHttpRequestBuilder().url(openApiUrl).build(), String.class).get();
        } catch (Exception e) {
            throw new ApplicationException(
                    "Could not fetch OpenAPI information from Kubernetes. Unexpected error occurred.", e);
        }

        // If return code is other than 200, fail
        if (!response.isSuccessful()) {
            throw new ApplicationException(
                    "Could not fetch OpenAPI information from Kubernetes. Make sure your Kubernetes cluster is running and the '{}' url is reachable. Returned code: '{}'",
                    openApiUrl,
                    response.code());
        }

        try {
            // Write the OpenAPI content to a temporary file
            Files.write(tempFile, response.body().getBytes());
        } catch (IOException e) {
            throw new ApplicationException(
                    "Could not write OpenAPI information to a file.", e);
        }

        consumer.accept(tempFile.toFile());

        try {
            // We can now delete the file
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            // Ignored
        }
    }

    /**
     * <p>
     * Construct the URL where the OpenAPI information is stored for the entire cluster.
     * </p>
     * 
     * @return The URL
     */
    private URL openApiUrl() {
        // 
        try {
            return new URL(kubernetesFacade.getMasterUrl(), "openapi/v2");
        } catch (MalformedURLException e) {
            throw new ApplicationException(
                    "Could not create temporary file. Unexpected error occurred.", e);
        }
    }

    private InputStream urlToStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new ApplicationException("Could not load resource from url: {}", url, e);
        }
    }

    public List<HasMetadata> installKubernetesResources(URL url, String namespace) {
        return installKubernetesResources(urlToStream(url), namespace);
    }

    public List<HasMetadata> installKubernetesResources(URL url, String namespace, Consumer<List<HasMetadata>> consumer) {
        return installKubernetesResources(urlToStream(url), namespace, consumer);
    }

    public List<HasMetadata> installKubernetesResources(InputStream stream, String namespace) {
        return installKubernetesResources(loadResources(stream, namespace), namespace);
    }

    public List<HasMetadata> installKubernetesResources(InputStream stream, String namespace,
            Consumer<List<HasMetadata>> consumer) {
        return installKubernetesResources(loadResources(stream, namespace), namespace, consumer);
    }

    public List<HasMetadata> installKubernetesResources(List<HasMetadata> resources, String namespace) {
        return installKubernetesResources(resources, namespace, null);
    }

    private List<HasMetadata> loadResources(InputStream stream, String namespace) {
        List<HasMetadata> resources = new ArrayList<>();

        LoaderOptions opts = new LoaderOptions();
        opts.setMaxAliasesForCollections(200);

        Yaml yaml = new Yaml(opts);
        Iterable<Object> elements = yaml.loadAll(stream);

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

    private List<HasMetadata> installKubernetesResources(List<HasMetadata> resources, String namespace,
            Consumer<List<HasMetadata>> consumer) {

        if (consumer != null) {
            consumer.accept(resources);
        }

        readMappings();

        List<HasMetadata> installed = new ArrayList<>();

        resources.forEach(resource -> {
            GenericKubernetesResource genericResource;

            // These are regular resources created in the code, we need to convert them into GenericKubernetesResource
            if (resource instanceof GenericKubernetesResource) {
                genericResource = (GenericKubernetesResource) resource;
            } else {
                genericResource = objectMapper.convertValue(resource, GenericKubernetesResource.class);
            }

            String resourceMapKey = genericResource.getApiVersion() + "/" + genericResource.getKind();

            // Group and version combined
            if (!genericResource.getApiVersion().contains("/")) {
                resourceMapKey = "/" + resourceMapKey;
            }

            ResourceMapping mapping = getResourceMapping(resourceMapKey);
            if (mapping == null) {
                throw new ApplicationException(
                        "Cannot install resource because it is unknown to the Kubernetes cluster: '{}'", genericResource);
            }

            OperationContext ctx = new OperationContext() //
                    .withClient(kubernetesFacade.getKubernetesClient()) //
                    .withPlural(mapping.getPlural()) //
                    .withPropagationPolicy(DeletionPropagation.FOREGROUND) //
                    .withApiGroupName(mapping.getGroup()) //
                    .withApiGroupVersion(genericResource.getApiVersion());

            // Namespaced
            if (mapping.isNamespaced()) {
                String ns = Objects.requireNonNullElse(genericResource.getMetadata().getNamespace(), namespace);

                // TODO: replace this with something more elegant
                if (HasMetadata.getGroup(Service.class).equals(mapping.getGroup())
                        && HasMetadata.getVersion(Service.class).equals(mapping.getVersion())
                        && HasMetadata.getKind(Service.class).equals(mapping.getKind())) {

                    // Find the existing resource
                    Service orig = kubernetesFacade.get(Service.class, ns, genericResource.getMetadata().getName());

                    Service service = objectMapper.convertValue(genericResource, Service.class);

                    if (orig != null) {
                        service = new ServiceBuilder(service)
                                .editSpec()
                                .withClusterIP(orig.getSpec().getClusterIP())
                                .endSpec()
                                .build();

                        installed.add(kubernetesFacade.update(service));
                        return;
                    }
                }

                installed.add(createResource(ctx, genericResource, namespace));

            } else {
                installed.add(createResource(ctx, genericResource, null));
            }
        });
        return installed;
    }

    private GenericKubernetesResource createResource(OperationContext ctx, GenericKubernetesResource resource,
            String namespace) {
        GenericKubernetesResourceOperationsImpl op = new GenericKubernetesResourceOperationsImpl(ctx, namespace != null);
        if (namespace != null) {
            op.inNamespace(namespace);
        }

        RetryConfig config = RetryConfig.<Boolean> custom()
                .maxAttempts(MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(MAX_INTERVAL))
                .retryExceptions(KubernetesClientException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("generic-resources");
        retry.getEventPublisher()
                .onRetry(e -> LOG.info("⏳ WAIT: Trying to apply {} again", Helper.getStatusLine(resource)));

        Supplier<GenericKubernetesResource> decorated = Retry.decorateSupplier(retry, () -> {
            return op.createOrReplace(resource);
        });
        return decorated.get();
    }

    private ResourceMapping getResourceMapping(String key) {
        RetryConfig config = RetryConfig.<Boolean> custom()
                .maxAttempts(MAX_RETRIES)
                .waitDuration(Duration.ofSeconds(MAX_INTERVAL))
                .retryOnResult(Void -> !resourceMappings.keySet().contains(key))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("mappings");
        retry.getEventPublisher()
                .onRetry(e -> LOG.info("⏳ WAIT: Waiting for the mapping'{}' to be available on the server", key));
        Runnable decorated = Retry.decorateRunnable(retry, () -> {
            readMappings();
        });
        decorated.run();

        return resourceMappings.get(key);
    }
}
