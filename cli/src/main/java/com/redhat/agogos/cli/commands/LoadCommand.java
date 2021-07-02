package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.errors.ApplicationException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "load", aliases = {
        "l" }, description = "Load agogos descriptors from files")
public class LoadCommand implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LoadCommand.class);

    @Option(names = { "-f", "--file" }, required = true, paramLabel = "FILE", description = "Paths to descriptors to load")
    List<Path> paths;

    @Option(names = { "--namespace",
            "-n" }, defaultValue = "default", required = true, description = "Namespace where Agogos resources should be installed, by default: ${DEFAULT-VALUE}.")
    String namespace;

    @Inject
    ResourceLoader resourceLoader;

    @Override
    public void run() {
        paths.forEach(path -> {
            LOG.info("🕞 Installing resources from '{}' file...", path);

            byte[] content = readFile(path);

            List<HasMetadata> resources = resourceLoader.installKubernetesResources(new ByteArrayInputStream(content),
                    namespace);

            Helper.status(resources);

            LOG.info("✅ {} resources installed", resources.size());
        });

    }

    private byte[] readFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ApplicationException("Could not read resources at {}", path, e);
        }
    }

}
