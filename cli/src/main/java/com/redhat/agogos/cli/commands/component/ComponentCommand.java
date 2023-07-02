package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "components", aliases = {
        "component", "c" }, description = "Interact with components", subcommands = { // 
                ComponentBuildCommand.class,
                ComponentListCommand.class,
                ComponentDescribeCommand.class
        })
public class ComponentCommand extends AbstractSubcommand {
}
