package com.redhat.agogos.cli.commands;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true, name = "load", aliases = {
        "l" }, description = "Load agogos descriptors from files")
public class LoadCommand implements Runnable {

    @Option(names = { "-f", "--file" }, paramLabel = "FILE", description = "Paths to descriptors to load")
    List<Path> files;

    @Inject
    AgogosClient agogosClient;

    @Inject
    KubernetesClient kubernetesClient;

    LoadCommand() {
        KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(Component.class),
                HasMetadata.getKind(Component.class),
                Component.class);
    }

    @Override
    public void run() {

        files.forEach(file -> {
            byte[] content = null;

            try {
                content = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new ApplicationException("Could not read resources", e);
            }

            List<HasMetadata> resources = null;

            try {
                resources = kubernetesClient.load(new ByteArrayInputStream(content)).inNamespace("default").get();
            } catch (KubernetesClientException e) {
                throw new ApplicationException("Could not read resources", e);
            }

            System.out.println(resources);
        });
    }

}
