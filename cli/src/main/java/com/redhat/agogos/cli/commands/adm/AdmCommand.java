package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "adm", description = "Agogos administration commands", subcommands = {
        InstallCommand.class,
        InitNamespaceCommand.class,
        LoadCommand.class
})
public class AdmCommand extends AbstractSubcommand {
}
