package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.commands.adm.InitCommand;
import com.redhat.agogos.cli.commands.adm.InstallCommand;
import com.redhat.agogos.cli.commands.base.AbstractCommand;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "adm", description = "Agogos administration commands", subcommands = { // 
        InstallCommand.class,
        InitCommand.class
})
public class AdmCommand extends AbstractCommand {
}
