package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.commands.AbstractCallableSubcommand;
import com.redhat.agogos.cli.commands.adm.install.*;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Comparator;

@Command(mixinStandardHelpOptions = true, name = "install", description = "Install Agogos")
public class InstallCommand extends AbstractCallableSubcommand {

    public static enum InstallProfile {
        dev,
        local,
        prod
    }

    @Option(names = { "--profile", "-p" }, description = {
            "Selected installation profile, valid values: ${COMPLETION-CANDIDATES}.\n", "Available profiles:\n",
            "* dev: everything is installed in the cluster where you are already logged in, Agogos resources are meant to be run outside of the cluster in the development environment, self-signed certificates are generated and can be used to connect to cluster",
            "* local: everything is installed in the cluster where you are already logged in, Agogos is deployed in the 'agogos' namespace, self-signed certificates are used",
            "* prod: everything is installed in the cluster where you are already logged in, Agogos is deployed in the 'agogos' namespace," })
    InstallProfile profile = InstallProfile.dev;

    @Option(names = { "--namespace",
            "-n" }, defaultValue = "agogos", description = "Namespace where Agogos resources should be installed, by default: ${DEFAULT-VALUE}.")
    String namespace;

    @Option(names = { "--skip-tekton", "-st" }, description = "Skip Tekton installation.", defaultValue = "false")
    public static Boolean skipTekton;

    @Option(names = { "--skip-knative", "-sk" }, description = "Skip Knative Eventing installation.", defaultValue = "false")
    public static Boolean skipKnative;

    @Option(names = { "--skip-tekton-chains",
            "-stc" }, description = "Skip Tekton chains installation.", defaultValue = "false")
    public static Boolean skipTektonChains;

    /**
     * This flag is useful for testing, where the statuses of resources are not always in the correct state considering the fact
     * that we are using a mocked Kubernetes server.
     */
    @Option(names = {
            "--no-wait" }, defaultValue = "false", description = "Flag to skip waiting for resource to be created.", hidden = true)
    boolean noWait;

    @Inject
    @Any
    Instance<Installer> installers;

    /**
     * Installs Agogos using selected profile.
     */
    @Override
    public Integer call() {
        helper.printStdout(String.format("💻 Selected profile: %s", profile));

        SortInstallers si = new SortInstallers();
        installers.stream()
                .filter(i -> inProfile(profile, i))
                .filter(i -> !(skipTekton && (i instanceof TektonInstaller || i instanceof TektonTriggersInstaller)))
                .filter(i -> !(skipKnative && i instanceof KnativeEventingInstaller))
                .filter(i -> !(skipTektonChains && (i instanceof TektonChainsInstaller)))
                .sorted((a, b) -> si.compare(a, b))
                .forEach(installer -> {
                    installer.install(profile, namespace);
                });

        return CommandLine.ExitCode.OK;
    }

    /**
     * <p>
     * Returns priority for the installer set by the {@link Priority} annotation.
     * </p>
     * 
     * @param installer
     * @return
     */
    private int getPriority(Installer installer) {
        Class<?> clazz = installer.getClass();

        while (clazz.isSynthetic()) {
            clazz = clazz.getSuperclass();
        }

        return clazz.getAnnotation(Priority.class).value();
    }

    /**
     * <p>
     * Checks whether the installer should be run in the particular profile selected in the CLI.
     * </p>
     * 
     * @param profile
     * @param clazz
     * @return
     */
    private boolean inProfile(InstallProfile profile, Installer installer) {
        Class<?> clazz = installer.getClass();

        while (clazz.isSynthetic()) {
            clazz = clazz.getSuperclass();
        }

        Profiles profiles = clazz.getAnnotation(Profiles.class);

        if (profiles != null) {
            for (Profile p : profiles.value()) {
                if (p.value().equals(profile)) {
                    return true;
                }
            }
        }

        Profile p = clazz.getAnnotation(Profile.class);

        if (p != null) {
            if (p.value().equals(profile)) {
                return true;
            }
        }

        return false;
    }

    class SortInstallers implements Comparator<Installer> {
        public int compare(Installer a, Installer b) {
            if (getPriority(a) != getPriority(b)) {
                return Integer.compare(getPriority(a), getPriority(b));
            } else {
                return a.getClass().getName().compareTo(b.getClass().getName());
            }
        }
    }
}
