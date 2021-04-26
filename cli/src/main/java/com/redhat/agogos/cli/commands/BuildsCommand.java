package com.redhat.agogos.cli.commands;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.commands.BuildsCommand.DescribeCommand;
import com.redhat.agogos.cli.commands.BuildsCommand.ListCommand;
import com.redhat.agogos.cli.commands.base.AbstractDescribeCommand;
import com.redhat.agogos.cli.commands.base.AbstractListCommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.BuildList;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "builds", aliases = {
        "build" }, description = "Interact with builds", subcommands = { // 
                DescribeCommand.class,
                ListCommand.class
        })
public class BuildsCommand implements Runnable {

    @Inject
    ListCommand listCommand;

    @Override
    public void run() {
        listCommand.run();
    }

    @Command(name = "describe", description = "describe build")
    static class DescribeCommand extends AbstractDescribeCommand<Build> {
        @Parameters(index = "0", description = "Name of the build")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            Build build = agogosClient.v1alpha1().builds().withName(name).get();
            this.describe(build);
        }
    }

    @Command(name = "list", description = "list builds")
    static class ListCommand extends AbstractListCommand<Build> {

        @Option(names = "--limit", description = "Number of items to display")
        long limit = 5;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            List<Build> builds = agogosClient.v1alpha1().builds()
                    .list(new ListOptionsBuilder().withNewLimit(limit)
                            .build())
                    .getItems();

            this.list(builds);
        }

        public void paginate() {
            String cont = null;

            BuildList buildList;

            do {
                buildList = agogosClient.v1alpha1().builds()
                        .list(new ListOptionsBuilder().withNewLimit(limit).withContinue(cont)
                                .build());

                buildList.getItems().forEach(build -> {
                    System.out.printf("%-20s %s\n", build.getMetadata().getNamespace(), build.getMetadata().getName());
                });

                cont = buildList.getMetadata().getContinue();
            } while (buildList.getMetadata().getRemainingItemCount() != null);
        }
    }
}
