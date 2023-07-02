package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = {
        "builds", "b" }, description = "Interact with builds", subcommands = {
                BuildDescribeCommand.class,
                BuildListCommand.class
        })

public class BuildCommand extends AbstractSubcommand {

    @Command(name = "foobar", description = "I'm a subcommand of `build`")
    int foobar() {
        System.out.println("I am the foobar subcommand");
        return 3;
    }
}
