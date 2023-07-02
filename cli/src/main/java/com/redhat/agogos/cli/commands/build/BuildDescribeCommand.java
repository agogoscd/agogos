package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.v1alpha1.Build;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Comparator;
import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "d", "desc" }, description = "describe build")
public class BuildDescribeCommand extends AbstractResourceSubcommand<Build> {

    @Option(names = { "-l", "--last" }, description = "Show description for last build")
    boolean last;

    @Parameters(arity = "0..1", description = "Name of the build")
    String name;

    @Override
    public void run() {
        Build build;

        if (last) {
            List<Build> resources = agogosClient.v1alpha1().builds().inNamespace(agogosClient.namespace()).list()
                    .getItems();

            build = resources.stream().sorted(byCreationTime()).findFirst().get();
        } else {
            build = agogosClient.v1alpha1().builds().inNamespace(agogosClient.namespace()).withName(name).get();
        }

        showResource(build);
    }

    Comparator<Build> byCreationTime() {
        return (r1, r2) -> {
            return r1.creationTime().compareTo(r2.creationTime());
        };
    }
}