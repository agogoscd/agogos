package com.redhat.agogos.cli.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Component;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "apply", aliases = {
        "a" }, description = "Create or update Agogos resources")
public class ApplyCommand implements Runnable {
    @Inject
    CLI cli;

    @Parameters(index = "0", description = "Path to file with one or more Agogos definitions.")
    File file;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    AgogosClient agogosClient;

    ApplyCommand() {
        System.out.println("AAA");
        KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(Component.class),
                HasMetadata.getKind(Component.class),
                Component.class);
    }

    @Override
    public void run() {
        List<HasMetadata> resources = null;

        try {
            resources = kubernetesClient.load(new FileInputStream(file)).inNamespace(kubernetesClient.getNamespace()).get();
            //  agogosClient.v1alpha1().components().inNamespace(agogosClient.namespace()).load(file).get();
        } catch (KubernetesClientException | FileNotFoundException e) {
            throw new ApplicationException("Could not load components from '{}' file", file.getAbsolutePath(), e);
        }

        try {
            //agogosClient.v1alpha1().components().createOrReplace(resources);
        } catch (KubernetesClientException e) {
            throw new ApplicationException("Could not apply components from '{}' file: '{}'", file.getAbsolutePath(),
                    e.getStatus().getMessage(), e);
        }
    }

}
