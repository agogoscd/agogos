package com.redhat.agogos.cli;

import javax.inject.Inject;

import com.redhat.agogos.cli.commands.BuildsCommand;
import com.redhat.agogos.cli.commands.InstallCommand;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = { //
        InstallCommand.class,
        BuildsCommand.class
})
public class CLI implements QuarkusApplication {

    @Inject
    CommandLine.IFactory factory;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    }
}
