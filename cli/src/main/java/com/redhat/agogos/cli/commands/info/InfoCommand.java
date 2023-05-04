package com.redhat.agogos.cli.commands.info;

import com.redhat.agogos.cli.commands.AbstractCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

@Command(mixinStandardHelpOptions = true, name = "info", aliases = { "i" }, description = "Agogos cluster information")
public class InfoCommand extends AbstractCommand {

    @Override
    public void run() {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append(Ansi.AUTO.string("💖 @|bold Agogos Information|@")).append(nl).append(nl);

        sb.append(
                Ansi.AUTO.string(String.format("@|bold Cluster URL|@:\t\t%s",
                        kubernetesClient.getMasterUrl())))
                .append(nl);
        sb.append(
                Ansi.AUTO.string(String.format("@|bold Namespace|@:\t\t%s",
                        kubernetesClient.getNamespace())))
                .append(nl);
        sb.append(
                Ansi.AUTO.string(String.format("@|bold Kubernetes version|@:\t%s",
                        String.format("%s.%s",
                                kubernetesClient.getKubernetesVersion().getMajor(),
                                kubernetesClient.getKubernetesVersion().getMinor()))))
                .append(nl);

        spec.commandLine().getOut().println(sb.toString());

    }
}
