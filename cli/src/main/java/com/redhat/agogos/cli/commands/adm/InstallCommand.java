package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.commands.AbstractCommand;
import com.redhat.agogos.cli.commands.adm.install.Installer;
import com.redhat.agogos.cli.commands.adm.install.Priority;
import com.redhat.agogos.cli.commands.adm.install.Profile;
import com.redhat.agogos.cli.commands.adm.install.Profiles;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Comparator;

@Command(mixinStandardHelpOptions = true, name = "install", description = "Install Agogos")
public class InstallCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(InstallCommand.class);

    public static enum InstallProfile {
        local,
        dev,
    }

    @Option(names = { "--profile", "-p" }, description = {
            "Selected installation profile, valid values: ${COMPLETION-CANDIDATES}.\n", "Available profiles:\n",
            "* local: everything is installed in the cluster you are already logged-in, Agogos is deployed in the 'agogos' namespace, self-signed certificates are used",
            "* dev: everything is installed in the cluster you are already logged-in, Agogos resources are meant to be run outside of the cluster in the development environment, self-signed certificates are generated and can be used to connect to cluster" })
    InstallProfile profile = InstallProfile.dev;

    @Option(names = { "--namespace",
            "-n" }, defaultValue = "agogos", description = "Namespace where Agogos resources should be installed, by default: ${DEFAULT-VALUE}.")
    String namespace;

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
    public void run() {
        LOG.info("💻 Selected profile: {}", profile);

        installers.stream()
                .filter(i -> inProfile(profile, i))
                .sorted(Comparator.comparingInt(i -> getPriority(i)))
                .forEach(installer -> installer.install(profile, namespace));

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

}
