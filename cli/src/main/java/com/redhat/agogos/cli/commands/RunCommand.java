package com.redhat.agogos.cli.commands;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentListCommand;
import com.redhat.agogos.cli.commands.RunCommand.RunListCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.ListMixin;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Run;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(mixinStandardHelpOptions = true, name = "runs", aliases = {
        "run", "r" }, description = "Interact with runs", subcommands = { // 
                RunListCommand.class
        })
public class RunCommand implements Runnable {
    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.run(ComponentListCommand.class);
    }

    @Command(mixinStandardHelpOptions = true, name = "list", description = "list runs")
    static class RunListCommand extends BaseCommand<Run> {

        @Mixin
        ListMixin list;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            List<Run> runs = agogosClient.v1alpha1().runs().list()
                    .getItems();

            print(runs);
        }
    }
}
