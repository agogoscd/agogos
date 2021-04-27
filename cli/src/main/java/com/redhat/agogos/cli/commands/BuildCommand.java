package com.redhat.agogos.cli.commands;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.BuildCommand.BuildDescribeCommand;
import com.redhat.agogos.cli.commands.BuildCommand.BuildListCommand;
import com.redhat.agogos.cli.commands.base.ListMixin;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = {
        "builds", "b" }, description = "Interact with builds", subcommands = { // 
                BuildDescribeCommand.class,
                BuildListCommand.class
        })
public class BuildCommand implements Runnable {

    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.run(BuildListCommand.class);
    }

    @Command(mixinStandardHelpOptions = true, name = "describe", description = "describe build")
    public static class BuildDescribeCommand extends BaseCommand<Build> {
        @Parameters(index = "0", description = "Name of the build")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            Build build = agogosClient.v1alpha1().builds().withName(name).get();
            print(build);
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", description = "list builds")
    static class BuildListCommand extends BaseCommand<Build> {

        @Mixin
        ListMixin list;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            List<Build> builds = agogosClient.v1alpha1().builds()
                    .list(new ListOptionsBuilder().withNewLimit(list.getLimit())
                            .build())
                    .getItems();

            print(builds);
        }

        // public void paginate() {
        //     String cont = null;

        //     BuildList buildList;

        //     do {
        //         buildList = agogosClient.v1alpha1().builds()
        //                 .list(new ListOptionsBuilder().withNewLimit(limit).withContinue(cont)
        //                         .build());

        //         buildList.getItems().forEach(build -> {
        //             System.out.printf("%-20s %s\n", build.getMetadata().getNamespace(), build.getMetadata().getName());
        //         });

        //         cont = buildList.getMetadata().getContinue();
        //     } while (buildList.getMetadata().getRemainingItemCount() != null);
        // }
    }
}
