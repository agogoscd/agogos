package com.redhat.agogos.cli.commands.info;

import com.redhat.agogos.cli.commands.AbstractCallableSubcommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

@Command(mixinStandardHelpOptions = true, name = "info", aliases = { "i" }, description = "Agogos cluster information")
public class InfoCommand extends AbstractCallableSubcommand {

    @Override
    public Integer call() {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append(Ansi.AUTO.string("💖 @|bold Agogos Information|@")).append(nl).append(nl);

        sb.append(
                Ansi.AUTO.string(String.format("@|bold Cluster URL|@:\t\t%s",
                        kubernetesFacade.getMasterUrl())))
                .append(nl);
        sb.append(
                Ansi.AUTO.string(String.format("@|bold Namespace|@:\t\t%s",
                        kubernetesFacade.getNamespace())))
                .append(nl);
        sb.append(
                Ansi.AUTO.string(String.format("@|bold Kubernetes version|@:\t%s",
                        String.format("%s.%s",
                                kubernetesFacade.getKubernetesVersion().getMajor(),
                                kubernetesFacade.getKubernetesVersion().getMinor()))))
                .append(nl);

        helper.printStdout(sb.toString());
        return CommandLine.ExitCode.OK;
    }
}
