package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.commands.AbstractCommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "adm", description = "Agogos administration commands", subcommands = { // 
        InstallCommand.class,
        InitCommand.class
})
public class AdmCommand extends AbstractCommand {
}
