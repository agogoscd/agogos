package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.commands.BuilderCommand.BuilderListCommand;
import com.redhat.agogos.cli.commands.base.AbstractCommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.v1alpha1.Builder;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "builder", aliases = {
        "builders", "bldr" }, description = "Interact with builders", subcommands = { // 
                BuilderListCommand.class
        })
public class BuilderCommand extends AbstractCommand {

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list builders")
    static class BuilderListCommand extends BaseListCommand<Builder> {

    }
}
