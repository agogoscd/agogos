package com.redhat.agogos.cli.commands;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentListCommand;
import com.redhat.agogos.cli.commands.TriggerCommand.TriggerListCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.ListMixin;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.triggers.Trigger;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(mixinStandardHelpOptions = true, name = "triggers", aliases = {
        "trigger", "t" }, description = "Interact with triggers", subcommands = { // 
                TriggerListCommand.class
        })
public class TriggerCommand implements Runnable {
    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.run(ComponentListCommand.class);
    }

    @Command(mixinStandardHelpOptions = true, name = "list", description = "list triggers")
    static class TriggerListCommand extends BaseCommand<Trigger> {

        @Mixin
        ListMixin list;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            List<Trigger> triggers = agogosClient.v1alpha1().triggers().list()
                    .getItems();

            print(triggers);
        }
    }
}
