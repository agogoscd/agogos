package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.StageCommand.StageListCommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.v1alpha1.Stage;
import picocli.CommandLine.Command;

import javax.inject.Inject;

@Command(mixinStandardHelpOptions = true, name = "stage", aliases = {
        "stages", "stg" }, description = "Interact with stages", subcommands = { // 
                StageListCommand.class
        })
public class StageCommand implements Runnable {

    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.usage(this.getClass());
    }

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list stages")
    static class StageListCommand extends BaseListCommand<Stage> {

    }
}
