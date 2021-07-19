package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.adm.InitCommand;
import com.redhat.agogos.cli.commands.adm.InstallCommand;
import picocli.CommandLine.Command;

import javax.inject.Inject;

@Command(mixinStandardHelpOptions = true, name = "adm", description = "Agogos administration commands", subcommands = { // 
        InstallCommand.class,
        InitCommand.class
})
public class AdmCommand implements Runnable {

    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.usage(AdmCommand.class);
    }

}